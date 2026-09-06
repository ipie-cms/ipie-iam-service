package in.gov.ipie.service.iam.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A one-time authorisation to set the initial password on an account that was provisioned without
 * credentials. Holds only the token's fingerprint - see {@code CredentialSetupTokens} for why the
 * token itself is never stored.
 *
 * @param consumedAt when this token was spent, or {@code null} if it has not been
 */
public record CredentialSetupToken(
        String tokenHash, UUID keycloakUserId, Instant issuedAt, Instant expiresAt, Instant consumedAt) {

    /**
     * Whether this token may still be spent.
     *
     * <p>Both conditions matter and for different reasons: expiry bounds how long a link left in a
     * mailbox stays dangerous, and single use stops a link that has already done its job from being
     * replayed by anyone who reaches that mailbox later - including the legitimate owner, whose
     * later password changes must go through the authenticated change-password path instead.
     */
    public boolean isSpendableAt(Instant now) {
        return consumedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }

    /** This token, marked spent at {@code now}. */
    public CredentialSetupToken consumedAt(Instant now) {
        return new CredentialSetupToken(tokenHash, keycloakUserId, issuedAt, expiresAt, now);
    }
}
