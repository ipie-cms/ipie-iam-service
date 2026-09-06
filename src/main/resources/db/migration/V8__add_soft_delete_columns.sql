-- Standard soft-delete columns (master standards doc, 7.2 / Database Mandatory Controls) alongside
-- the existing audit columns. `roles` and `permissions` already have created_at/created_by/
-- updated_at/updated_by/version (V6) - this migration adds the three new soft-delete columns to
-- both, nullable/defaulted so existing rows stay valid (expand-only migration).

ALTER TABLE roles
    ADD COLUMN is_active  BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by VARCHAR(100);

ALTER TABLE permissions
    ADD COLUMN is_active  BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by VARCHAR(100);

-- user_roles has never had the standard audit columns (only its own assigned_at/assigned_by) -
-- UserRoleJpaEntity now extends AuditableJpaEntity, so all 8 columns are new here. Existing rows
-- backfill created_at/updated_at from assigned_at and created_by/updated_by from assigned_by,
-- since a role assignment's "creation" and "assignment" are the same event for rows that predate
-- this column set.
ALTER TABLE user_roles
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN version    BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN is_active  BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by VARCHAR(100);

UPDATE user_roles SET created_at = assigned_at, created_by = assigned_by,
                       updated_at = assigned_at, updated_by = assigned_by
WHERE created_at IS NULL;

ALTER TABLE user_roles
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL;
