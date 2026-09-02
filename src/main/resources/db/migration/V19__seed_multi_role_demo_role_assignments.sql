-- Role assignments for the multi-role demonstration account seeded in ipie-user-service V6.
--
-- V18 gave that account a credential and stopped there, so it could log in and then had nothing to
-- log in *to*: every other seeded demo user carries rows here (V7 for the original four, V13 for the
-- FRS logins), and this one carried none. The effect was visible in exactly the place the account
-- exists to demonstrate - GET /api/v1/users/me/roles returned an empty list, and the dashboard,
-- which renders that call, read "No roles assigned yet." on the one fixture seeded to prove
-- multiplicity. Nothing in Keycloak papers over it: the realm entry's roles reach the token, but
-- this service answers from these tables, not from the token.
--
-- WHY THESE THREE. INSOLVENCY_PROFESSIONAL and REGISTERED_VALUER mirror the two qualifications
-- ipie-user-service V6 seeds for the same person, so the RBAC view and the professional-registration
-- view agree rather than each telling a different story about who they are. STAKEHOLDER comes with
-- them for the same reason it does in V7 and V13: it is the base grant every case stakeholder
-- holds, and it is where DASHBOARD_VIEW comes from - without it the account would authenticate and
-- still not be entitled to the dashboard it lands on.
--
-- STILL NO CAPACITY ROLE, and deliberately, on the same reasoning as V6 and the realm entry: IRP,
-- RP, Liquidator and AR are held on a case by an NCLT order, not by a person (ARCHITECTURE_WORKING_PLAN.md
-- 10.1). A standing row here would state as permanent something a single order changes mid-case.
--
-- ON CONFLICT because this migration is expected to meet databases where the rows were inserted by
-- hand to repair a running environment before it existed.
--
-- LIKE V18 AND user-service V6, THIS MUST NOT REACH PRODUCTION - it seeds a known demonstration
-- account, and belongs in a development-only Flyway location once the platform grows one.

INSERT INTO user_roles (user_id, keycloak_user_id, role_id, assigned_at, assigned_by, created_at, created_by, updated_at, updated_by) VALUES
    ('10000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000001', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),  -- STAKEHOLDER
    ('10000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000004', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed'),  -- INSOLVENCY_PROFESSIONAL
    ('10000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000006', now(), 'flyway-seed', now(), 'flyway-seed', now(), 'flyway-seed')   -- REGISTERED_VALUER
ON CONFLICT (user_id, role_id) DO NOTHING;
