package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.domain.CredentialSetupToken;
import in.gov.ipie.service.iam.repository.CredentialSetupTokenRepository;

/**
 * Adapts {@link CredentialSetupTokenRepository} onto Spring Data JPA - the only class that touches
 * {@link CredentialSetupTokenJpaRepository} or {@link CredentialSetupTokenEntity}.
 *
 * <p>No {@code @Transactional} here; callers on {@code CredentialService} are transactional. The
 * modifying query behind {@link #deleteExpiredBefore} requires an active transaction to run at all.
 */
@Repository
class CredentialSetupTokenRepositoryImpl implements CredentialSetupTokenRepository {

    private final CredentialSetupTokenJpaRepository jpaRepository;

    CredentialSetupTokenRepositoryImpl(CredentialSetupTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<CredentialSetupToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findById(tokenHash).map(CredentialSetupTokenRepositoryImpl::toDomain);
    }

    @Override
    public CredentialSetupToken save(CredentialSetupToken token) {
        CredentialSetupTokenEntity entity = jpaRepository.findById(token.tokenHash())
                .map(existing -> {
                    if (token.consumedAt() != null) {
                        existing.consume(token.consumedAt());
                    }
                    return existing;
                })
                .orElseGet(() -> new CredentialSetupTokenEntity(
                        token.tokenHash(), token.keycloakUserId(), token.issuedAt(),
                        token.expiresAt(), token.consumedAt()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void consumeOutstandingFor(UUID keycloakUserId, Instant now) {
        jpaRepository.findByKeycloakUserIdAndConsumedAtIsNull(keycloakUserId)
                .forEach(entity -> entity.consume(now));
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jpaRepository.deleteByExpiresAtBefore(cutoff);
    }

    private static CredentialSetupToken toDomain(CredentialSetupTokenEntity entity) {
        return new CredentialSetupToken(
                entity.getTokenHash(), entity.getKeycloakUserId(), entity.getIssuedAt(),
                entity.getExpiresAt(), entity.getConsumedAt());
    }
}
