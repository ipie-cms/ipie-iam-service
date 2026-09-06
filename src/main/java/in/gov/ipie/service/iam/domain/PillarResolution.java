package in.gov.ipie.service.iam.domain;

import java.util.UUID;

/** What {@code /internal/pillar-links/resolve} needs - the service's public return shape, decoupled from the JPA entity. */
public record PillarResolution(UUID ipieUserId, UUID keycloakUserId, boolean verified) {
}
