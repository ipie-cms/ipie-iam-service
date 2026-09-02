package in.gov.ipie.service.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.event.UserVerifiedEvent;
import in.gov.ipie.service.iam.service.RoleService;

/**
 * Kafka counterpart to {@link UserVerifiedEventConsumer} - auto-assigns the default role when
 * ipie-user-service publishes {@code USER_VERIFIED}, active only when this service is configured
 * to use Kafka instead of RabbitMQ.
 *
 * <p>Unlike the RabbitMQ side (a dedicated queue bound to just this routing key, see {@code
 * UserServiceEventConsumerConfig}), a Kafka topic carries every event type ipie-user-service
 * publishes - {@code EventConsumerConfig}'s consumer factory always deserializes into a raw
 * {@link EventEnvelope}{@code <?>} (the same generic-payload shape {@code UserEventLogConsumer}
 * already receives), so this listener subscribes to the whole topic and filters to {@code
 * USER_VERIFIED} itself, converting the matching payload with {@code
 * ObjectMapper.convertValue(...)} rather than {@code readValue(...)} - {@code
 * event.data()} arrives already deserialized once (a {@code LinkedHashMap} at this generic
 * boundary), so this is an object-graph conversion, not a JSON-string parse (same reasoning as
 * {@code AuditEventCodec}).
 */
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
class KafkaUserVerifiedEventConsumer {

    private static final String EVENT_TYPE = "USER_VERIFIED";

    private static final Logger LOG = LoggerFactory.getLogger(KafkaUserVerifiedEventConsumer.class);

    private final RoleService roleService;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;
    private final String defaultRoleName;

    KafkaUserVerifiedEventConsumer(
            RoleService roleService,
            ProcessedEventStore processedEventStore,
            ObjectMapper objectMapper,
            @Value("${ipie.integrations.user-service.default-role:STAKEHOLDER}") String defaultRoleName) {
        this.roleService = roleService;
        this.processedEventStore = processedEventStore;
        this.objectMapper = objectMapper;
        this.defaultRoleName = defaultRoleName;
    }

    @KafkaListener(
            topics = "${ipie.integrations.user-service.kafka.topic:ipie-user-service.events}",
            groupId = "${spring.application.name}.user-verified")
    void onEvent(EventEnvelope<?> event) {
        if (!EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> {
            UserVerifiedEvent payload = objectMapper.convertValue(event.data(), UserVerifiedEvent.class);
            LOG.info(
                    "Auto-assigning default role {} to user {} (keycloak {})",
                    defaultRoleName, payload.userId(), payload.keycloakUserId());
            roleService.assignDefaultRole(payload.userId(), payload.keycloakUserId(), defaultRoleName);
        });
    }
}
