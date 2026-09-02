package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

/**
 * Read-optimised projection of ipie-user-service's authoritative {@code pillar_links} table
 * (ADR-001, "Placement of pillar_links Data and /resolve Endpoint") - deliberately minimal,
 * only what {@code /internal/pillar-links/resolve} needs on the hot login path. Kept current
 * by {@link PillarResolutionEventConsumer}, not written to directly by any controller.
 */
@Entity
@Table(name = "pillar_resolution")
class PillarResolutionEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(name = "pillar_type", nullable = false, length = 20)
    private String pillarType;

    @Column(name = "external_pillar_id", nullable = false, length = 100)
    private String externalPillarId;

    @Column(name = "ipie_user_id", nullable = false)
    private UUID ipieUserId;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected PillarResolutionEntity() {
        // required by JPA
    }

    PillarResolutionEntity(
            String pillarType, String externalPillarId, UUID ipieUserId, UUID keycloakUserId, boolean verified,
            Instant syncedAt) {
        this.pillarType = pillarType;
        this.externalPillarId = externalPillarId;
        this.ipieUserId = ipieUserId;
        this.keycloakUserId = keycloakUserId;
        this.verified = verified;
        this.syncedAt = syncedAt;
    }

    UUID getId() {
        return id;
    }

    String getPillarType() {
        return pillarType;
    }

    String getExternalPillarId() {
        return externalPillarId;
    }

    UUID getIpieUserId() {
        return ipieUserId;
    }

    void setIpieUserId(UUID ipieUserId) {
        this.ipieUserId = ipieUserId;
    }

    UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    void setKeycloakUserId(UUID keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    boolean isVerified() {
        return verified;
    }

    void setVerified(boolean verified) {
        this.verified = verified;
    }

    void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
