package in.gov.ipie.service.iam.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.iam.domain.PillarResolution;

/**
 * Domain-owned port for the {@code pillar_resolution} CQRS projection - the read path every
 * pillar-SSO login goes through, kept in step with {@code ipie-user-service}'s
 * {@code pillar_links} by {@code ACCOUNT_LINKED}/{@code ACCOUNT_UNLINKED} events.
 *
 * <p>Introduced when the former {@code resolution/} package was dissolved into the platform's
 * standard packages (master standards doc, sections 3 and 5). Before that, the service called the
 * Spring Data JPA interface and its {@code @Entity} directly - both package-private siblings in
 * {@code resolution/}. Splitting them apart would have meant making those JPA types {@code public}
 * purely to keep a cross-layer call compiling, which section 5 explicitly discourages ("by
 * convention, only the matching {@code *RepositoryImpl} references its {@code *JpaRepository}").
 * This port keeps the persistence types package-private and mirrors the {@code RoleRepository} /
 * {@code RoleRepositoryImpl} pair one package over.
 */
public interface PillarResolutionRepository {

    /** The projection for a pillar identity, or empty when that identity is not linked. */
    Optional<PillarResolution> findBy(String pillarType, String externalPillarId);

    /**
     * Inserts the projection row, or updates the existing one in place when this pillar
     * identity is already projected. The find-or-create sits in the adapter rather than the service
     * so the JPA entity never leaves {@code persistence}.
     */
    void upsert(
            String pillarType, String externalPillarId, UUID ipieUserId, UUID keycloakUserId, boolean verified,
            Instant syncedAt);

    /** Removes the projection for a pillar identity. A no-op when nothing is projected. */
    void deleteBy(String pillarType, String externalPillarId);
}
