-- Backs ProcessedEventStore (common-events): tracks which inbound event ids this service has
-- already handled, so a redelivered Kafka message is not processed twice (master standards doc,
-- section 9: "Consumers must handle duplicate delivery"). A service-owned table, per "each
-- service owns its data" (7.1) - same reasoning as iam_idempotency_keys on the synchronous API
-- side. Named iam_processed_events, not processed_events - see V2's comment on the shared
-- database with ipie-user-service.
CREATE TABLE iam_processed_events (
    event_id     VARCHAR(128) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
