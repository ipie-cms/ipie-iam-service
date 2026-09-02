package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping for {@code credential_setup_tokens}. The primary key is the token's SHA-256
 * fingerprint - the token itself is never persisted, so a read of this table yields nothing usable.
 */
@Entity
@Table(name = "credential_setup_tokens")
class CredentialSetupTokenEntity {

    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected CredentialSetupTokenEntity() {
    }

    CredentialSetupTokenEntity(String tokenHash, UUID keycloakUserId, Instant issuedAt, Instant expiresAt, Instant consumedAt) {
        this.tokenHash = tokenHash;
        this.keycloakUserId = keycloakUserId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
    }

    String getTokenHash() {
        return tokenHash;
    }

    UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getConsumedAt() {
        return consumedAt;
    }

    void consume(Instant now) {
        this.consumedAt = now;
    }
}
