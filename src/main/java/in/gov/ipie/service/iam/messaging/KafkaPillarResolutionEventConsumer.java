package in.gov.ipie.service.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.service.PillarResolutionEventHandler;
import in.gov.ipie.service.iam.event.AccountLinkedEvent;
import in.gov.ipie.service.iam.event.AccountUnlinkedEvent;

/**
 * Kafka counterpart to {@link PillarResolutionEventConsumer} - active only when this service
 * is configured to use Kafka instead of RabbitMQ. A single Kafka topic carries every event type
 * ipie-user-service publishes (unlike RabbitMQ's dedicated queue per routing key), so this
 * listener subscribes once and dispatches on {@code eventType()} itself - see {@link
 * KafkaUserVerifiedEventConsumer}'s Javadoc for the same reasoning applied to a single-event
 * listener. Delegates to the same {@link PillarResolutionEventHandler} the RabbitMQ side
 * uses, so the upsert/delete + audit-recording logic itself is never duplicated.
 */
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
class KafkaPillarResolutionEventConsumer {

    private static final String ACCOUNT_LINKED = "ACCOUNT_LINKED";
    private static final String ACCOUNT_UNLINKED = "ACCOUNT_UNLINKED";

    private final PillarResolutionEventHandler handler;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    KafkaPillarResolutionEventConsumer(
            PillarResolutionEventHandler handler, ProcessedEventStore processedEventStore, ObjectMapper objectMapper) {
        this.handler = handler;
        this.processedEventStore = processedEventStore;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${ipie.integrations.user-service.kafka.topic:ipie-user-service.events}",
            groupId = "${spring.application.name}.pillar-resolution")
    void onEvent(EventEnvelope<?> event) {
        if (ACCOUNT_LINKED.equals(event.eventType())) {
            IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> handler.handleAccountLinked(
                    objectMapper.convertValue(event.data(), AccountLinkedEvent.class), event.correlationId()));
        } else if (ACCOUNT_UNLINKED.equals(event.eventType())) {
            IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> handler.handleAccountUnlinked(
                    objectMapper.convertValue(event.data(), AccountUnlinkedEvent.class), event.correlationId()));
        }
    }
}
