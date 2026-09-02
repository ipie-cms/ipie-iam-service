package in.gov.ipie.service.iam.event;

import java.util.UUID;

/**
 * Mirrors ipie-user-service's {@code UserVerifiedPayload} - the shape of the {@code
 * EventEnvelope.data} field for the {@code USER_VERIFIED} event this service consumes from
 * ipie-user-service's own exchange/topic. Services do not share a contracts module in this
 * platform (see the other reference consumers), so this is a local, duck-typed copy of the
 * publisher's payload shape.
 */
public record UserVerifiedEvent(UUID userId, UUID keycloakUserId, String email) {
}
