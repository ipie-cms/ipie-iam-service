-- Backs OutboxStore (common-events): the transactional outbox pattern. UserService.publish
-- writes a row here in the SAME database transaction as the business change it describes, so the
-- two either both commit or both roll back - no dual-write risk between Postgres and Kafka. A
-- separate poller (OutboxRelayScheduler) later reads unpublished rows and hands them to the real
-- EventPublisher, so the business transaction never talks to the broker directly. Named
-- iam_outbox_events, not outbox_events - see V2's comment on the shared database with
-- ipie-user-service.
CREATE TABLE iam_outbox_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    payload      TEXT NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

-- Supports the relay's "find the oldest unpublished rows" poll query.
CREATE INDEX idx_iam_outbox_events_unpublished ON iam_outbox_events (occurred_at) WHERE published_at IS NULL;
