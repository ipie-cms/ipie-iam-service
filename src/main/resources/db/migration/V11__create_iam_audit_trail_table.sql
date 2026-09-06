-- Queryable, persisted counterpart to every AuditEvent this service records - see
-- ipie-user-service's V16__create_audit_trail_table.sql for the full reasoning, identical here.
-- Named iam_audit_trail, not audit_trail - this service shares ipie-user-service's physical
-- database (Database Mandatory Controls' User/IAM shared-database exception), same
-- table-prefixing convention as iam_idempotency_keys/iam_processed_events/iam_outbox_events.

CREATE TABLE iam_audit_trail (
    id                UUID PRIMARY KEY,
    event_type        VARCHAR(20) NOT NULL,
    action            VARCHAR(100) NOT NULL,
    entity_type       VARCHAR(100) NOT NULL,
    entity_id         VARCHAR(100),
    case_id           VARCHAR(100),
    actor_user_id     VARCHAR(100),
    source_ip         VARCHAR(45),
    service_name      VARCHAR(100) NOT NULL,
    comment           VARCHAR(500),
    old_value         TEXT,
    new_value         TEXT,
    correlation_id    VARCHAR(100),
    occurred_at       TIMESTAMPTZ NOT NULL,
    persisted_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_iam_audit_trail_correlation_id ON iam_audit_trail (correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_iam_audit_trail_entity_type_id ON iam_audit_trail (entity_type, entity_id);
