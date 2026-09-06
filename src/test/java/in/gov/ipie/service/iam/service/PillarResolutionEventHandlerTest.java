package in.gov.ipie.service.iam.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.audit.AuditRecorder;
import in.gov.ipie.service.iam.event.AccountLinkedPayload;
import in.gov.ipie.service.iam.event.AccountUnlinkedPayload;

/**
 * Shared upsert/delete + audit-recording logic both {@code PillarResolutionConsumer}
 * (RabbitMQ) and {@code KafkaPillarResolutionConsumer} delegate to - see this class's
 * Javadoc.
 */
class PillarResolutionEventHandlerTest {

    private final PillarResolutionServiceImpl resolutionService = mock(PillarResolutionServiceImpl.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final PillarResolutionEventHandler handler =
            new PillarResolutionEventHandler(resolutionService, auditRecorder, "ipie-iam-service");

    @Test
    void handleAccountLinked_upsertsTheProjection_andRecordsAnAuditEventUnderTheInboundCorrelationId() {
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        AccountLinkedPayload payload = new AccountLinkedPayload(userId, keycloakUserId, "IBBI", "ext-1", true, Instant.now());

        handler.handleAccountLinked(payload, "corr-1");

        verify(resolutionService).upsert("IBBI", "ext-1", userId, keycloakUserId, true);
        verify(auditRecorder).record(argThat(event -> "corr-1".equals(event.correlationId())
                && "PILLAR_RESOLUTION_UPSERTED".equals(event.action())
                && userId.toString().equals(event.entityId())));
    }

    @Test
    void handleAccountUnlinked_deletesTheProjection_andRecordsAnAuditEventUnderTheInboundCorrelationId() {
        UUID userId = UUID.randomUUID();
        AccountUnlinkedPayload payload = new AccountUnlinkedPayload(userId, "IBBI", "ext-1");

        handler.handleAccountUnlinked(payload, "corr-2");

        verify(resolutionService).delete("IBBI", "ext-1");
        verify(auditRecorder).record(argThat(event -> "corr-2".equals(event.correlationId())
                && "PILLAR_RESOLUTION_DELETED".equals(event.action())
                && userId.toString().equals(event.entityId())));
    }

    @Test
    void handleAccountLinked_stillUpserts_whenAuditRecordingFails() {
        UUID userId = UUID.randomUUID();
        AccountLinkedPayload payload = new AccountLinkedPayload(userId, UUID.randomUUID(), "IBBI", "ext-1", true, Instant.now());
        doThrow(new RuntimeException("outbox unavailable")).when(auditRecorder).record(any());

        handler.handleAccountLinked(payload, "corr-3");

        verify(resolutionService).upsert(eq("IBBI"), eq("ext-1"), eq(userId), any(), eq(true));
    }
}
