package in.gov.ipie.service.iam.event;

import java.util.UUID;

/** Mirrors ipie-user-service's {@code AccountUnlinkedPayload} - see {@link AccountLinkedPayload}'s Javadoc. */
public record AccountUnlinkedPayload(UUID userId, String pillarType, String externalPillarId) {
}
