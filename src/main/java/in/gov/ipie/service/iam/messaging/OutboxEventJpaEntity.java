package in.gov.ipie.service.iam.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import in.gov.ipie.common.events.jpa.AbstractOutboxEventJpaEntity;

/**
 * Where this service's outbox rows live. The row shape and every query over it belong to the
 * platform ({@link AbstractOutboxEventJpaEntity}); the table name is the one thing that is genuinely
 * this service's own, prefixed because this service shares ipie-user-service's physical database
 * (EX-001) and the platform tables would otherwise collide.
 */
@Entity
@Table(name = "iam_outbox_events")
class OutboxEventJpaEntity extends AbstractOutboxEventJpaEntity {
}
