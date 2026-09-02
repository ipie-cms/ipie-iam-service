package in.gov.ipie.service.iam.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.service.iam.domain.PillarResolution;
import in.gov.ipie.service.iam.repository.PillarResolutionRepository;

/**
 * Owns the {@code pillar_resolution} projection: upserted/deleted by {@code
 * PillarResolutionEventConsumer} as {@code AccountLinked}/{@code AccountUnlinkedEvent} arrive,
 * read by {@code PillarLinkResolveController} on the login hot path (ADR-001). One indexed
 * query, zero cross-service calls at read time - the entire point of this projection.
 *
 * <p>Depends on the {@link PillarResolutionRepository} port, not on Spring Data or the JPA
 * entity. Until the former {@code resolution/} package was dissolved into the platform's standard
 * packages (master standards doc, sections 3 and 5), this class reached straight into both.
 *
 * <p><b>Transactions.</b> Each public method here is the transaction boundary; the adapter declares
 * none of its own, so the find-then-write inside {@link #upsert} is one unit of work rather than two
 * autocommits that another consumer thread could interleave with. {@link #resolve} is
 * {@code readOnly} - it runs on every pillar-SSO login, and the flag lets Hibernate skip
 * dirty-checking and the driver take a read-only connection.
 */
@Service
public class PillarResolutionService {

    private final PillarResolutionRepository repository;

    public PillarResolutionService(PillarResolutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @CacheEvict(cacheNames = "pillar-resolution", key = "#pillarType + ':' + #externalPillarId")
    public void upsert(String pillarType, String externalPillarId, UUID ipieUserId, UUID keycloakUserId, boolean verified) {
        repository.upsert(pillarType, externalPillarId, ipieUserId, keycloakUserId, verified, Instant.now());
    }

    @Transactional
    @CacheEvict(cacheNames = "pillar-resolution", key = "#pillarType + ':' + #externalPillarId")
    public void delete(String pillarType, String externalPillarId) {
        repository.deleteBy(pillarType, externalPillarId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "pillar-resolution", key = "#pillarType + ':' + #externalPillarId",
            unless = "#result == null || #result.isEmpty()")
    public Optional<PillarResolution> resolve(String pillarType, String externalPillarId) {
        return repository.findBy(pillarType, externalPillarId);
    }
}
