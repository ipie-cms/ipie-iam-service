package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping for {@code user_credentials}. Package-private, like every entity in this package, so
 * the persistence model cannot leak past {@link UserCredentialRepositoryImpl}.
 *
 * <p>No {@code AuditableJpaEntity} base and no soft-delete columns: a credential row has exactly one
 * meaningful timestamp, and a soft-deleted password hash is a password hash that is still there.
 * Removing a credential means removing the row.
 */
@Entity
@Table(name = "user_credentials")
class UserCredentialEntity {

    @Id
    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String algorithm;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserCredentialEntity() {
    }

    UserCredentialEntity(UUID keycloakUserId, String passwordHash, String algorithm, Instant updatedAt) {
        this.keycloakUserId = keycloakUserId;
        this.passwordHash = passwordHash;
        this.algorithm = algorithm;
        this.updatedAt = updatedAt;
    }

    UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getAlgorithm() {
        return algorithm;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void update(String newPasswordHash, String newAlgorithm, Instant now) {
        this.passwordHash = newPasswordHash;
        this.algorithm = newAlgorithm;
        this.updatedAt = now;
    }
}
