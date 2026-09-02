package in.gov.ipie.service.iam.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.audit.AuditRecorder;
import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.service.iam.event.AccountLinkedEvent;
import in.gov.ipie.service.iam.event.AccountUnlinkedEvent;

/**
 * The broker-independent core of {@code pillar_resolution} projection sync - upsert/delete
 * plus the manual {@link AuditEvent} recording (see {@link PillarResolutionEventConsumer}'s
 * own Javadoc for why this is recorded manually rather than via {@code @Auditable}). Extracted out
 * of {@link PillarResolutionEventConsumer} (RabbitMQ) so {@link
 * KafkaPillarResolutionEventConsumer} can share the exact same logic rather than a second,
 * independently-maintained copy of the audit-recording boilerplate - the two consumer classes
 * differ only in how they receive the event (a dedicated RabbitMQ queue per routing key vs. a
 * single Kafka topic filtered in-method), not in what happens once one arrives.
 */
@Component
public class PillarResolutionEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PillarResolutionEventHandler.class);

    private final PillarResolutionService resolutionService;
    private final AuditRecorder auditRecorder;
    private final String serviceName;

    public PillarResolutionEventHandler(
            PillarResolutionService resolutionService, AuditRecorder auditRecorder,
            @Value("${spring.application.name}") String serviceName) {
        this.resolutionService = resolutionService;
        this.auditRecorder = auditRecorder;
        this.serviceName = serviceName;
    }

    public void handleAccountLinked(AccountLinkedEvent payload, String correlationId) {
        LOG.info(
                "Upserting pillar_resolution for {}/{} -> user {}",
                payload.pillarType(), payload.externalPillarId(), payload.userId());
        resolutionService.upsert(
                payload.pillarType(), payload.externalPillarId(), payload.userId(), payload.keycloakUserId(),
                payload.verified());
        recordAudit("PILLAR_RESOLUTION_UPSERTED", payload.userId().toString(), payload, correlationId);
    }

    public void handleAccountUnlinked(AccountUnlinkedEvent payload, String correlationId) {
        LOG.info("Removing pillar_resolution for {}/{}", payload.pillarType(), payload.externalPillarId());
        resolutionService.delete(payload.pillarType(), payload.externalPillarId());
        recordAudit("PILLAR_RESOLUTION_DELETED", payload.userId().toString(), payload, correlationId);
    }

    private void recordAudit(String action, String entityId, Object newValue, String correlationId) {
        try {
            auditRecorder.record(new AuditEvent(
                    AuditEventType.BUSINESS, action, "PILLAR_RESOLUTION", entityId, null, "system", null, serviceName,
                    null, null, newValue, correlationId, Instant.now()));
        } catch (RuntimeException auditFailure) {
            // An audit trail problem must not fail projection sync - the projection is already
            // correct at this point, only the audit record failed.
            LOG.error("Failed to record audit event for action={}", action, auditFailure);
        }
    }
}
