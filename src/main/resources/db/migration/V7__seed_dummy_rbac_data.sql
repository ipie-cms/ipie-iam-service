-- Dummy roles/permissions/assignments, consistent with ipie-user-service's own seed
-- (V9__seed_dummy_registrations.sql) and deploy/keycloak/realm-export.json's seeded realm roles
-- and users, so the whole flow can be demoed without walking through the UI first.

INSERT INTO permissions (id, name, description, resource, created_at, created_by, updated_at, updated_by) VALUES
    ('30000000-0000-0000-0000-000000000001', 'DASHBOARD_VIEW', 'View the stakeholder dashboard', 'DASHBOARD', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('30000000-0000-0000-0000-000000000002', 'CLAIMS_READ', 'Read access to claims', 'CLAIMS', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('30000000-0000-0000-0000-000000000003', 'CLAIMS_WRITE', 'Submit/update claims', 'CLAIMS', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('30000000-0000-0000-0000-000000000004', 'REGISTRATIONS_VERIFY', 'Verify pending user registrations', 'REGISTRATIONS', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('30000000-0000-0000-0000-000000000005', 'ROLES_MANAGE', 'Create roles and assign them to users', 'IAM', now(), 'flyway-seed', now(), 'flyway-seed');

INSERT INTO roles (id, name, description, created_at, created_by, updated_at, updated_by) VALUES
    ('40000000-0000-0000-0000-000000000001', 'STAKEHOLDER', 'Default role auto-assigned on registration verification', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000002', 'STAKEHOLDER_ADMIN', 'Can verify pending registrations and manage roles', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000003', 'CREDITOR', 'IBC creditor stakeholder', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000004', 'INSOLVENCY_PROFESSIONAL', 'IBC insolvency professional stakeholder', now(), 'flyway-seed', now(), 'flyway-seed');

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000004'),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000005'),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003'),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000003');

-- user_id/keycloak_user_id values match ipie-user-service's V9__seed_dummy_registrations.sql
-- (U1-U3, all VERIFIED) and deploy/keycloak/realm-export.json's seeded users (K1-K3). U4
-- (UNVERIFIED) and U5 (PRE_REGISTRATION) intentionally have no role assignment yet.
INSERT INTO user_roles (user_id, keycloak_user_id, role_id, assigned_at, assigned_by) VALUES
    ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000003', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000004', now(), 'flyway-seed');
