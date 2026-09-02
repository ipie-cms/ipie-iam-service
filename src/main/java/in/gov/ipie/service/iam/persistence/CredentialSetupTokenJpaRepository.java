package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CredentialSetupTokenJpaRepository extends JpaRepository<CredentialSetupTokenEntity, String> {

    List<CredentialSetupTokenEntity> findByKeycloakUserIdAndConsumedAtIsNull(UUID keycloakUserId);

    /**
     * Bulk delete for the retention sweep - a derived {@code deleteBy...} would load every matching
     * row into the persistence context first, which is the wrong shape for a housekeeping job that
     * may match a great many rows at once.
     */
    @Modifying
    @Query("delete from CredentialSetupTokenEntity t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
