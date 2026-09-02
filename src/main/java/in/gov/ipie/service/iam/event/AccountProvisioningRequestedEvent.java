package in.gov.ipie.service.iam.event;

import java.util.UUID;

/**
 * This service's own copy of the ipie-user-service event that asks for a Keycloak account, mirroring
 * how {@link UserVerifiedEvent} and the account-link events are handled - each service owns its view
 * of a contract rather than sharing a class across a repository boundary.
 *
 * <p>Deliberately has no password field, and adding one would be a mistake: the publishing side
 * keeps credentials off this event so they are never written to an outbox table or relayed through a
 * broker. The account is created without credentials and the user sets a password later.
 */
public record AccountProvisioningRequestedEvent(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName) {
}
