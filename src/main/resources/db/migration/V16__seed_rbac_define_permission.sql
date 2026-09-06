-- Splits "manage roles" into the two powers it had been conflating.
--
-- ROLES_MANAGE (V7) gated both *defining* the RBAC catalogue - creating, editing and deleting
-- roles - and *assigning* an existing role to a user. STAKEHOLDER_ADMIN holds it, so an admin could
-- compose a role granting any permission in the catalogue and then hold it. Assigning a role hands
-- out a capability someone already decided the platform should have; defining one decides what
-- capabilities exist at all, and a role composed with the wrong permissions grants them to every
-- holder at once. Those are not the same power and should not be the same permission.
--
-- After this migration:
--   RBAC_DEFINE   POST/PUT/DELETE /api/v1/roles, POST /api/v1/permissions   SUPER_ADMIN only
--   ROLES_MANAGE  POST/DELETE/GET /api/v1/users/{userId}/roles              STAKEHOLDER_ADMIN + SUPER_ADMIN
--
-- ROLES_MANAGE keeps its name rather than being renamed to ROLES_ASSIGN. It is a Keycloak realm
-- role carried in every issued token and named in realm-export.json's composites; renaming it here
-- would strip access from everyone holding a current token until it was re-issued. Read it as
-- "manage a user's roles", not "manage the set of roles".

INSERT INTO permissions (id, name, description, resource, created_at, created_by, updated_at, updated_by) VALUES
    ('30000000-0000-0000-0000-000000000006', 'RBAC_DEFINE', 'Define the RBAC catalogue: create/edit/delete roles and create permissions', 'IAM', now(), 'flyway-seed', now(), 'flyway-seed');

-- SUPER_ADMIN only, and deliberately not STAKEHOLDER_ADMIN - that is the entire point of the split.
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000006');  -- SUPER_ADMIN -> RBAC_DEFINE

-- The DB grant above is only half of it. RoleService/KeycloakUserManagementClient only ever push a
-- role's bare *name* to Keycloak, never its role_permissions rows, so a DB-side grant does not
-- appear in an issued token by itself: the realm role and the SUPER_ADMIN composite have to be
-- mirrored by hand in deploy/keycloak/realm-export.json. That is done in the same change as this
-- migration. This is exactly how CREDENTIAL_VERIFY came to be enforced-but-ungranted and returned
-- 403 on every login until the realm side was added.
