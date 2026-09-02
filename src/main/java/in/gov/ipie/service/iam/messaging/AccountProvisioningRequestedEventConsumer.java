package in.gov.ipie.service.iam.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.service.iam.command.ProvisionAccountCommand;
import in.gov.ipie.service.iam.event.AccountCredentialSetupRequestedPayload;
import in.gov.ipie.service.iam.event.AccountProvisionedPayload;
import in.gov.ipie.service.iam.event.AccountProvisioningRequestedEvent;
import in.gov.ipie.service.iam.event.IamEventType;
import in.gov.ipie.service.iam.service.AccountProvisioningService;
import in.gov.ipie.service.iam.service.CredentialService;

/**
 * Creates the Keycloak account ipie-user-service asked for, then reports the id back.
 *
 * <p>This is the asynchronous half of registration. It replaces a synchronous call that sat on the
 * user's critical path, where a busy or briefly unavailable Keycloak failed the registration
 * outright. Here the same failure means the message is retried and, if it keeps failing, lands in
 * {@code ipie.events.dlq} - the registration stays in {@code PROVISIONING} and is recoverable,
 * rather than the citizen being told to start again.
 *
 * <p><b>The account is created with no password</b>, and Keycloak never holds one
 * (ARCHITECTURE_WORKING_PLAN.md, D1) - the hash lives in this service. The request event carries no
 * credential and must not: a password on an event would be written to the outbox and relayed through
 * the broker.
 *
 * <p>So this handler publishes <b>two</b> events, deliberately separate. One asks
 * ipie-communication-service to email the registrant a one-time link for choosing their password;
 * it carries the setup token and goes straight to comms. The other tells ipie-user-service the
 * account exists, carries no token, and is what releases the pillar-admin approval email.
 *
 * <p>Two events rather than one because they authorise different things and reach different
 * mailboxes. Conflating them is precisely the mistake that dead-ended the earlier design, where a
 * single token was expected to serve both the registrant and the approving admin, and the only email
 * carrying it went to the admin.
 *
 * <p>Idempotent on the event id, because delivery is at-least-once and creating a second Keycloak
 * account - or a second live setup token - for one registration is not something a retry should be
 * able to do.
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class AccountProvisioningRequestedEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AccountProvisioningRequestedEventConsumer.class);

    private final AccountProvisioningService accountProvisioningService;
    private final CredentialService credentialService;
    private final ProcessedEventStore processedEventStore;
    private final OutboxStore outboxStore;
    private final String serviceName;

    AccountProvisioningRequestedEventConsumer(
            AccountProvisioningService accountProvisioningService,
            CredentialService credentialService,
            ProcessedEventStore processedEventStore,
            OutboxStore outboxStore,
            @Value("${spring.application.name}") String serviceName) {
        this.accountProvisioningService = accountProvisioningService;
        this.credentialService = credentialService;
        this.processedEventStore = processedEventStore;
        this.outboxStore = outboxStore;
        this.serviceName = serviceName;
    }

    @RabbitListener(queues = "${ipie.integrations.user-service.rabbitmq.account-provisioning-queue:"
            + "ipie-iam-service.events.account-provisioning-requested}")
    @Transactional
    void onAccountProvisioningRequested(EventEnvelope<AccountProvisioningRequestedEvent> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> {
            AccountProvisioningRequestedEvent payload = event.data();
            LOG.info("Provisioning Keycloak account for user {}", payload.userId());

            UUID keycloakUserId = accountProvisioningService.provisionAccount(new ProvisionAccountCommand(
                    payload.userId(), payload.username(), payload.email(),
                    payload.firstName(), payload.lastName(), null));

            // The account exists but cannot be logged into - it has no credential. Mint the one-time
            // token that lets its owner set one, and ask comms to mail it. Straight to comms, not
            // via ipie-user-service: that service must never handle a credential-setting token.
            String setupToken = credentialService.issueSetupToken(keycloakUserId);
            outboxStore.save(EventEnvelope.create(
                    IamEventType.ACCOUNT_CREDENTIAL_SETUP_REQUESTED.name(), IamEventType.CONTRACT_VERSION,
                    serviceName, LoggingContext.correlationId(), null,
                    new AccountCredentialSetupRequestedPayload(
                            payload.userId(), keycloakUserId, payload.email(),
                            fullName(payload.firstName(), payload.lastName()), setupToken)));

            // Separately, and without the token: this is what moves the registration on and releases
            // the pillar-admin approval email.
            outboxStore.save(EventEnvelope.create(
                    IamEventType.ACCOUNT_PROVISIONED.name(), IamEventType.CONTRACT_VERSION, serviceName,
                    LoggingContext.correlationId(), null,
                    new AccountProvisionedPayload(payload.userId(), keycloakUserId)));
        });
    }

    /**
     * Recomposes the display name for the email greeting. The request event carries the split form
     * because that is what Keycloak's user representation needs; a person reading an email wants it
     * whole. Tolerates either half being absent - a mononym is a real name, not a validation error.
     */
    private static String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }
}
