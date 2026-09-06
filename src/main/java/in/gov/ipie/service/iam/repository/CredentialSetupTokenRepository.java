package in.gov.ipie.service.iam.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.iam.domain.CredentialSetupToken;

/** Persistence contract for one-time credential-setup tokens. */
public interface CredentialSetupTokenRepository {

    /** Looked up by fingerprint, never by the token itself - see {@code CredentialSetupTokens}. */
    Optional<CredentialSetupToken> findByTokenHash(String tokenHash);

    CredentialSetupToken save(CredentialSetupToken token);

    /**
     * Marks every outstanding token for this account as spent. Called before issuing a new one, so
     * that "send me another link" invalidates the previous email rather than leaving two live links
     * to the same account.
     */
    void consumeOutstandingFor(UUID keycloakUserId, Instant now);

    /** Retention sweep: removes tokens that expired before {@code cutoff}, spent or not. */
    int deleteExpiredBefore(Instant cutoff);
}
