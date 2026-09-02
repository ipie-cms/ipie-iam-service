package in.gov.ipie.service.iam.service;

import java.util.UUID;

/**
 * The platform's credential authority (ARCHITECTURE_WORKING_PLAN.md, D2). Every plaintext password in the
 * system passes through this interface and nowhere else - not ipie-user-service, not an event, not
 * the outbox, not Keycloak's database.
 */
public interface CredentialService {

    /**
     * Answers Keycloak's question on the login path: is this the right password for this account?
     *
     * <p>Returns a boolean rather than throwing, because both answers are ordinary outcomes here and
     * the caller is an authenticator that must render the same failure either way. An account with
     * no credential yet is simply {@code false} - it was provisioned without one and its owner has
     * not completed setup.
     */
    boolean verify(UUID keycloakUserId, String rawPassword);

    /**
     * Issues a one-time setup token for an account provisioned without credentials, invalidating any
     * outstanding token for it first, and returns the token in clear <b>once</b> - the caller is
     * expected to hand it straight to the notification that carries it and never persist it.
     *
     * <p>Only the fingerprint is stored, so this value is unrecoverable afterwards. Re-issuing means
     * calling this again, which is also what invalidates the earlier link.
     */
    String issueSetupToken(UUID keycloakUserId);

    /**
     * Sets the initial password for the account named by {@code token}, then spends the token.
     *
     * @return the Keycloak user id the token named - the caller has only the token, so this is the
     *     only way it learns whose password was just set
     * @throws in.gov.ipie.service.iam.exception.InvalidCredentialSetupTokenException if the token is
     *     unknown, expired, or already used
     */
    UUID setInitialPassword(String token, String rawPassword);

    /**
     * Changes an authenticated user's password, having first confirmed they know the current one.
     *
     * @throws in.gov.ipie.service.iam.exception.InvalidCurrentPasswordException if it does not match
     */
    void changePassword(UUID keycloakUserId, String currentPassword, String newPassword);
}
