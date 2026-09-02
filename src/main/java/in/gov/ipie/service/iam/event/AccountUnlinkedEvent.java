package in.gov.ipie.service.iam.event;

import java.util.UUID;

/** Mirrors ipie-user-service's {@code AccountUnlinkedPayload} - see {@link AccountLinkedEvent}'s Javadoc. */
public record AccountUnlinkedEvent(UUID userId, String pillarType, String externalPillarId) {
}
