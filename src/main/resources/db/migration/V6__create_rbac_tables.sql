-- RBAC/ABAC source of truth: roles and permissions are owned here (not in Keycloak) - role
-- definitions and user_roles assignments are synced to Keycloak realm roles by RoleService via
-- KeycloakUserManagementClient, so JWTs issued by Keycloak still carry the right "permissions"
-- claim, but this schema (not the realm config) is authoritative.
CREATE TABLE roles (
    id              UUID PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    description     VARCHAR(255),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100) NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- "resource" is the ABAC attribute axis a permission applies to (e.g. "CLAIMS", "DASHBOARD") -
-- not wired into any attribute-matching policy engine yet (see the platform's OPA scaffold for
-- that), but the schema already separates "what" (resource) from "which action" (name) so that
-- can be added later without a migration.
CREATE TABLE permissions (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    resource        VARCHAR(100) NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100) NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- user_id is ipie-user-service's own user id, not a foreign key - separate services, separate
-- databases (master standards doc, section on service boundaries: no cross-database FKs).
-- keycloak_user_id is also stored (not just user_id) because the only real query path - the
-- dashboard fetching "my roles" - only ever has the JWT's own `sub` claim (the Keycloak user id)
-- to work with, never ipie-user-service's internal id, and looking that up would mean an extra
-- cross-service call on every dashboard load. user_id remains the primary key (the stable
-- application identity, from ipie-user-service) for audit/bookkeeping purposes.
CREATE TABLE user_roles (
    user_id             UUID NOT NULL,
    keycloak_user_id    UUID NOT NULL,
    role_id             UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by         VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_keycloak_user_id ON user_roles (keycloak_user_id);
