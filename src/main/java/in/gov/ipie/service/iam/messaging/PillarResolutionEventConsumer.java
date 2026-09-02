package in.gov.ipie.service.iam.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.service.PillarResolutionEventHandler;
import in.gov.ipie.service.iam.event.AccountLinkedEvent;
import in.gov.ipie.service.iam.event.AccountUnlinkedEvent;

/**
 * Keeps {@code pillar_resolution} current as ipie-user-service publishes {@code
 * ACCOUNT_LINKED}/{@code ACCOUNT_UNLINKED} - the primary sync mechanism for the read projection
 * (ADR-001); a nightly reconciliation job comparing against the source of truth is the backstop,
 * not yet built (out of scope for this pass, see plan notes). RabbitMQ side - see {@link
 * KafkaPillarResolutionEventConsumer} for the Kafka counterpart and {@code
 * UserServiceEventConsumerConfig} for the queue/binding wiring. The actual upsert/delete + audit
 * logic lives in {@link PillarResolutionEventHandler}, shared by both broker-specific
 * classes - see its Javadoc for why, including the reasoning for recording the {@link AuditEvent}
 * manually rather than via {@code @Auditable}.
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class PillarResolutionEventConsumer {

    private final PillarResolutionEventHandler handler;
    private final ProcessedEventStore processedEventStore;

    PillarResolutionEventConsumer(PillarResolutionEventHandler handler, ProcessedEventStore processedEventStore) {
        this.handler = handler;
        this.processedEventStore = processedEventStore;
    }

    @RabbitListener(queues = "${ipie.integrations.user-service.rabbitmq.account-linked-queue:ipie-iam-service.events.account-linked}")
    void onAccountLinked(EventEnvelope<AccountLinkedEvent> event) {
        IdempotentEventHandler.handle(
                event.eventId(), processedEventStore, () -> handler.handleAccountLinked(event.data(), event.correlationId()));
    }

    @RabbitListener(queues = "${ipie.integrations.user-service.rabbitmq.account-unlinked-queue:ipie-iam-service.events.account-unlinked}")
    void onAccountUnlinked(EventEnvelope<AccountUnlinkedEvent> event) {
        IdempotentEventHandler.handle(
                event.eventId(), processedEventStore, () -> handler.handleAccountUnlinked(event.data(), event.correlationId()));
    }
}
