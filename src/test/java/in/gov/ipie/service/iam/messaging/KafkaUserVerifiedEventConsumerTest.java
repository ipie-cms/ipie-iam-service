package in.gov.ipie.service.iam.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.iam.event.UserVerifiedEvent;
import in.gov.ipie.service.iam.service.RoleService;

class KafkaUserVerifiedEventConsumerTest {

    private final RoleService roleService = mock(RoleService.class);
    private final ProcessedEventStore processedEventStore = mock(ProcessedEventStore.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaUserVerifiedEventConsumer consumer =
            new KafkaUserVerifiedEventConsumer(roleService, processedEventStore, objectMapper, "STAKEHOLDER");

    @Test
    void onEvent_assignsTheDefaultRole_forAMatchingEventType() {
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        EventEnvelope<UserVerifiedEvent> event = EventEnvelope.create(
                "USER_VERIFIED", 1, "ipie-user-service", null, null, new UserVerifiedEvent(userId, keycloakUserId, "a@b.com"));
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(roleService).assignDefaultRole(userId, keycloakUserId, "STAKEHOLDER");
    }

    @Test
    void onEvent_ignoresAnyOtherEventTypeOnTheSharedTopic() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "ipie-user-service", null, null, "user-1");

        consumer.onEvent(event);

        verify(roleService, never()).assignDefaultRole(any(), any(), any());
    }
}
