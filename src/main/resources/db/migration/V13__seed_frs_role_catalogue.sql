-- Seeds the remaining RFP FRS login types that had no role yet (Annexure 3, "List of Indicative
-- Dashboards"): RV/RVE Login, Legal Entity/Legal Representative Login, IBBI Senior Officials
-- Login, plus Authorized Rep from the registration form's Professional Role list. Matches
-- V7__seed_dummy_rbac_data.sql's pattern - each new role granted DASHBOARD_VIEW only, the same
-- minimal grant STAKEHOLDER itself has, since no richer permission vocabulary exists yet for
-- these domains.

INSERT INTO roles (id, name, description, created_at, created_by, updated_at, updated_by) VALUES
    ('40000000-0000-0000-0000-000000000006', 'REGISTERED_VALUER', 'IBC registered valuer stakeholder (RV/RVE Login)', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000007', 'LEGAL_REPRESENTATIVE', 'Legal entity/legal representative stakeholder', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000008', 'AUTHORIZED_REPRESENTATIVE', 'Authorized representative of a creditor/stakeholder class', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('40000000-0000-0000-0000-000000000009', 'IBBI_OFFICIAL', 'IBBI senior official - platform regulator, not a case stakeholder', now(), 'flyway-seed', now(), 'flyway-seed');

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000001'),  -- REGISTERED_VALUER -> DASHBOARD_VIEW
    ('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000001'),  -- LEGAL_REPRESENTATIVE -> DASHBOARD_VIEW
    ('40000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000001'),  -- AUTHORIZED_REPRESENTATIVE -> DASHBOARD_VIEW
    ('40000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000001');  -- IBBI_OFFICIAL -> DASHBOARD_VIEW

-- user_id/keycloak_user_id match ipie-user-service's V20__seed_frs_role_demo_users.sql (U7-U10)
-- and deploy/keycloak/realm-export.json's seeded users (K6-K9). REGISTERED_VALUER/
-- LEGAL_REPRESENTATIVE/AUTHORIZED_REPRESENTATIVE also get the base STAKEHOLDER role, matching
-- CREDITOR/INSOLVENCY_PROFESSIONAL's existing pattern (V7); IBBI_OFFICIAL stands alone, matching
-- STAKEHOLDER_ADMIN's pattern - a platform/regulator role, not a case stakeholder.
INSERT INTO user_roles (user_id, keycloak_user_id, role_id, assigned_at, assigned_by, created_at, created_by, updated_at, updated_by) VALUES
    ('10000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000006', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000007', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000008', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),
    ('10000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000009', '40000000-0000-0000-0000-000000000009', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed');
