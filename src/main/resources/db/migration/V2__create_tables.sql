-- iam-service does not own user identity (that's ipie-user-service) - only the generic
-- idempotency-key scaffolding from the template is kept here. RBAC tables (roles, permissions,
-- role_permissions, user_roles) are added in V6__create_rbac_tables.sql.
--
-- Named iam_idempotency_keys, not idempotency_keys - this service shares its database with
-- ipie-user-service (Database Mandatory Controls: User/IAM shared-database exception), which has
-- its own identically-purposed table under the un-prefixed name. This migration has never been
-- successfully applied against any persistent database (only Testcontainers, which never
-- persists), so renaming it here directly is safe - no environment has this table under its old
-- name to migrate away from.
CREATE TABLE iam_idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    response_status INTEGER NOT NULL,
    response_body   TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
