package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.domain.PillarResolution;
import in.gov.ipie.service.iam.repository.PillarResolutionRepository;

/**
 * Adapts {@link PillarResolutionRepository} onto Spring Data JPA. The only class that touches
 * {@link PillarResolutionJpaRepository} or {@link PillarResolutionEntity} - both stay
 * package-private, so the JPA types cannot leak past this package (master standards doc, section 5).
 *
 * <p>No {@code @Transactional} here by design: every caller is a {@code @Transactional} method on
 * {@code PillarResolutionService}, so these run inside the caller's transaction. That matters
 * for {@link #upsert} in particular - the find and the write must be one unit of work, and the
 * derived delete behind {@link #deleteBy} requires an active transaction to run at all.
 */
@Repository
class PillarResolutionRepositoryImpl implements PillarResolutionRepository {

    private final PillarResolutionJpaRepository jpaRepository;

    PillarResolutionRepositoryImpl(PillarResolutionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PillarResolution> findBy(String pillarType, String externalPillarId) {
        return jpaRepository.findByPillarTypeAndExternalPillarId(pillarType, externalPillarId)
                .map(PillarResolutionRepositoryImpl::toDomain);
    }

    @Override
    public void upsert(
            String pillarType, String externalPillarId, UUID ipieUserId, UUID keycloakUserId, boolean verified,
            Instant syncedAt) {
        PillarResolutionEntity entity = jpaRepository
                .findByPillarTypeAndExternalPillarId(pillarType, externalPillarId)
                .orElseGet(() -> new PillarResolutionEntity(
                        pillarType, externalPillarId, ipieUserId, keycloakUserId, verified, syncedAt));

        entity.setIpieUserId(ipieUserId);
        entity.setKeycloakUserId(keycloakUserId);
        entity.setVerified(verified);
        entity.setSyncedAt(syncedAt);

        jpaRepository.save(entity);
    }

    @Override
    public void deleteBy(String pillarType, String externalPillarId) {
        jpaRepository.deleteByPillarTypeAndExternalPillarId(pillarType, externalPillarId);
    }

    private static PillarResolution toDomain(PillarResolutionEntity entity) {
        return new PillarResolution(entity.getIpieUserId(), entity.getKeycloakUserId(), entity.isVerified());
    }
}
