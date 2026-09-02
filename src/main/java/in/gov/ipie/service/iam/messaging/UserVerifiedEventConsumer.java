package in.gov.ipie.service.iam.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.service.RoleService;
import in.gov.ipie.service.iam.event.UserVerifiedEvent;

/**
 * Auto-assigns the default role when ipie-user-service publishes {@code USER_VERIFIED} - the
 * cross-service half of the registration flow (see {@code UserServiceEventConsumerConfig}).
 * RabbitMQ-only for now (matches the only broker actually active in docker-compose today); a
 * Kafka equivalent would mirror {@code EventConsumerConfig}'s pattern if that broker is chosen
 * instead.
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class UserVerifiedEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UserVerifiedEventConsumer.class);

    private final RoleService roleService;
    private final ProcessedEventStore processedEventStore;
    private final String defaultRoleName;

    UserVerifiedEventConsumer(
            RoleService roleService,
            ProcessedEventStore processedEventStore,
            @Value("${ipie.integrations.user-service.default-role:STAKEHOLDER}") String defaultRoleName) {
        this.roleService = roleService;
        this.processedEventStore = processedEventStore;
        this.defaultRoleName = defaultRoleName;
    }

    @RabbitListener(queues = "${ipie.integrations.user-service.rabbitmq.user-verified-queue:ipie-iam-service.events.user-verified}")
    void onUserVerified(EventEnvelope<UserVerifiedEvent> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> {
            UserVerifiedEvent payload = event.data();
            LOG.info(
                    "Auto-assigning default role {} to user {} (keycloak {})",
                    defaultRoleName, payload.userId(), payload.keycloakUserId());
            roleService.assignDefaultRole(payload.userId(), payload.keycloakUserId(), defaultRoleName);
        });
    }
}
