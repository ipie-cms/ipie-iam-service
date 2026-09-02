package in.gov.ipie.service.iam.repository;

import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.iam.domain.UserCredential;

/** Persistence contract for stored password hashes (master standards doc, section 5's layering). */
public interface UserCredentialRepository {

    Optional<UserCredential> findByKeycloakUserId(UUID keycloakUserId);

    /** Inserts or replaces this account's credential - setting a password is idempotent by nature. */
    UserCredential save(UserCredential credential);

    /** Removes an account's credential, e.g. when the account itself is deleted (DPDP erasure). */
    void deleteByKeycloakUserId(UUID keycloakUserId);
}
