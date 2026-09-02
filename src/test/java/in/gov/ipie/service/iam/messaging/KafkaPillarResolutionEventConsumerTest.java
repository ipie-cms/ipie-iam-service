package in.gov.ipie.service.iam.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.service.PillarResolutionEventHandler;
import in.gov.ipie.service.iam.event.AccountLinkedEvent;
import in.gov.ipie.service.iam.event.AccountUnlinkedEvent;

class KafkaPillarResolutionEventConsumerTest {

    private final PillarResolutionEventHandler handler = mock(PillarResolutionEventHandler.class);
    private final ProcessedEventStore processedEventStore = mock(ProcessedEventStore.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final KafkaPillarResolutionEventConsumer consumer =
            new KafkaPillarResolutionEventConsumer(handler, processedEventStore, objectMapper);

    @Test
    void onEvent_delegatesAnAccountLinkedEvent_toTheSharedHandler() {
        UUID userId = UUID.randomUUID();
        AccountLinkedEvent payload = new AccountLinkedEvent(userId, UUID.randomUUID(), "IBBI", "ext-1", true, Instant.now());
        EventEnvelope<AccountLinkedEvent> event =
                EventEnvelope.create("ACCOUNT_LINKED", 1, "ipie-user-service", "corr-1", null, payload);
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(handler).handleAccountLinked(payload, "corr-1");
    }

    @Test
    void onEvent_delegatesAnAccountUnlinkedEvent_toTheSharedHandler() {
        AccountUnlinkedEvent payload = new AccountUnlinkedEvent(UUID.randomUUID(), "IBBI", "ext-1");
        EventEnvelope<AccountUnlinkedEvent> event =
                EventEnvelope.create("ACCOUNT_UNLINKED", 1, "ipie-user-service", "corr-2", null, payload);
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(handler).handleAccountUnlinked(payload, "corr-2");
    }

    @Test
    void onEvent_ignoresAnyOtherEventTypeOnTheSharedTopic() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "ipie-user-service", null, null, "user-1");

        consumer.onEvent(event);

        verify(handler, never()).handleAccountLinked(any(), any());
        verify(handler, never()).handleAccountUnlinked(any(), any());
    }
}
