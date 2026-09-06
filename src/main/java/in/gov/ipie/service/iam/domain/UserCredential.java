package in.gov.ipie.service.iam.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One account's password, as stored: a hash and the parameters it was made with. Never a password.
 *
 * <p>Keyed on the Keycloak user id because the caller on the login path is Keycloak's own
 * authenticator, which knows only its {@code UserModel} id. Keying on the ipie user id instead would
 * put ipie-user-service on every login just to translate an identifier.
 */
public record UserCredential(UUID keycloakUserId, String passwordHash, String algorithm, Instant updatedAt) {
}
