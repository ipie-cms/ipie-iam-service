package in.gov.ipie.service.iam.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.domain.UserCredential;
import in.gov.ipie.service.iam.repository.UserCredentialRepository;

/**
 * Adapts {@link UserCredentialRepository} onto Spring Data JPA - the only class that touches
 * {@link UserCredentialJpaRepository} or {@link UserCredentialEntity}.
 *
 * <p>No {@code @Transactional} here: every caller is a transactional method on
 * {@code CredentialService}, so these run inside the caller's unit of work. {@link #save} depends on
 * that - it reads before it writes, and the two must not be separate transactions.
 */
@Repository
class UserCredentialRepositoryImpl implements UserCredentialRepository {

    private final UserCredentialJpaRepository jpaRepository;

    UserCredentialRepositoryImpl(UserCredentialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserCredential> findByKeycloakUserId(UUID keycloakUserId) {
        return jpaRepository.findById(keycloakUserId).map(UserCredentialRepositoryImpl::toDomain);
    }

    @Override
    public UserCredential save(UserCredential credential) {
        // Update in place when the row exists, so JPA issues an UPDATE rather than trying to insert
        // over an existing primary key. Setting a password is naturally idempotent - the same call
        // twice must leave one row, not fail the second time.
        UserCredentialEntity entity = jpaRepository.findById(credential.keycloakUserId())
                .map(existing -> {
                    existing.update(credential.passwordHash(), credential.algorithm(), credential.updatedAt());
                    return existing;
                })
                .orElseGet(() -> new UserCredentialEntity(
                        credential.keycloakUserId(), credential.passwordHash(),
                        credential.algorithm(), credential.updatedAt()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteByKeycloakUserId(UUID keycloakUserId) {
        jpaRepository.deleteById(keycloakUserId);
    }

    private static UserCredential toDomain(UserCredentialEntity entity) {
        return new UserCredential(
                entity.getKeycloakUserId(), entity.getPasswordHash(), entity.getAlgorithm(), entity.getUpdatedAt());
    }
}
