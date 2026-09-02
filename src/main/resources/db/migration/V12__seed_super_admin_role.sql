-- Seeds SUPER_ADMIN, matching V7__seed_dummy_rbac_data.sql's pattern: a platform-operator role
-- sitting above STAKEHOLDER_ADMIN, granted every existing permission (it is the superuser), and
-- assigned to a demo user whose id matches ipie-user-service's V18__seed_super_admin_user.sql and
-- deploy/keycloak/realm-export.json's seeded superadmin@ipie.gov.in (K5, keycloak_user_id
-- 20000000-0000-0000-0000-000000000005).

INSERT INTO roles (id, name, description, created_at, created_by, updated_at, updated_by) VALUES
    ('40000000-0000-0000-0000-000000000005', 'SUPER_ADMIN', 'Platform operator (IBBI/MCA) - top of the role hierarchy, above STAKEHOLDER_ADMIN', now(), 'flyway-seed', now(), 'flyway-seed');

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001'),  -- SUPER_ADMIN -> DASHBOARD_VIEW
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002'),  -- SUPER_ADMIN -> CLAIMS_READ
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000003'),  -- SUPER_ADMIN -> CLAIMS_WRITE
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000004'),  -- SUPER_ADMIN -> REGISTRATIONS_VERIFY
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005');  -- SUPER_ADMIN -> ROLES_MANAGE

-- user_id/keycloak_user_id match ipie-user-service's V18__seed_super_admin_user.sql (U6) and
-- deploy/keycloak/realm-export.json's seeded superadmin@ipie.gov.in (K5). user_roles carries the
-- standard audit columns since V8 (UserRoleJpaEntity extends AuditableJpaEntity), unlike V7's
-- original insert which predates that migration.
INSERT INTO user_roles (user_id, keycloak_user_id, role_id, assigned_at, assigned_by, created_at, created_by, updated_at, updated_by) VALUES
    ('10000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000005', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed');
