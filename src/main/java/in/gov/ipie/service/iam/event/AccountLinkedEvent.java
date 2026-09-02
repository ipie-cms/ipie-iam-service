package in.gov.ipie.service.iam.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors ipie-user-service's {@code AccountLinkedPayload} - the shape of the {@code
 * EventEnvelope.data} field for {@code ACCOUNT_LINKED}, consumed to keep this service's
 * {@code pillar_resolution} read projection current (ADR-001). {@code pillarType} is a
 * plain {@code String} here, not a duplicated local enum - the two services' allowed values
 * already drifted out of sync once this session (a pillar type added to one without the
 * other's constraint being updated), so the projection intentionally does not maintain its own
 * copy of the valid-values list to keep in sync.
 */
public record AccountLinkedEvent(
        UUID userId, UUID keycloakUserId, String pillarType, String externalPillarId, boolean verified, Instant linkedAt) {
}
