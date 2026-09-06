package in.gov.ipie.service.iam.event;

import java.util.UUID;

/**
 * Reports that a Keycloak account now exists for {@code userId}.
 *
 * <p>The account is created without credentials, so it cannot be logged into yet - the user sets a
 * password through the verification email that ipie-user-service sends on receiving this. Carrying
 * the Keycloak id back is the whole point: the requesting service holds the pairing between its own
 * user id and the Keycloak subject, and nothing else can supply it.
 */
public record AccountProvisionedPayload(UUID userId, UUID keycloakUserId) {
}
