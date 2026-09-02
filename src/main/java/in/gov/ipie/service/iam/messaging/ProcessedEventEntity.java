package in.gov.ipie.service.iam.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import in.gov.ipie.common.events.jpa.AbstractProcessedEventEntity;

/**
 * Where this service records the event ids it has already acted on, so a redelivery does not act
 * twice. Shape and behaviour are the platform's; only the table name is this service's, prefixed because this service shares ipie-user-service's physical database (EX-001) and the platform tables would otherwise collide.
 */
@Entity
@Table(name = "iam_processed_events")
class ProcessedEventEntity extends AbstractProcessedEventEntity {
}
