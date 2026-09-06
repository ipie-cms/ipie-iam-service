-- iam_idempotency_keys backed the per-service, DB-based @Idempotent implementation - replaced by
-- ipie-common-libs' shared, Redis-backed IdempotencyStore (in.gov.ipie.common.web.idempotency),
-- which every service now gets automatically instead of maintaining its own storage. Unlike this
-- table (unbounded, no TTL, no cleanup), the replacement always expires an entry
-- (ipie.idempotency.ttl). A new migration, not an edit to V2__create_tables.sql, out of caution -
-- V2's own comment documents that editing-in-place is only safe once a migration is confirmed to
-- have never shipped against a real, persistent environment; that has not been independently
-- reconfirmed here.

DROP TABLE iam_idempotency_keys;
