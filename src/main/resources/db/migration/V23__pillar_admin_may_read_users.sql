-- Gives PILLAR_ADMIN the permission that lets it list users at all, and begins reconciling the two
-- permission vocabularies recorded as open in 10.3.
--
-- THE TIER DID NOT WORK WITHOUT THIS. A pillar admin administers the users of their pillar, but
-- GET /api/v1/users is gated by USER_READ - which existed only as a Keycloak realm role and was
-- absent from this service's catalogue, so no role defined here could ever grant it. The endpoint
-- answered 403 before any visibility rule was consulted, which made the scoping work look broken
-- when it was unreachable.
--
-- USER_READ, and deliberately not USER_WRITE or USER_DELETE. "See what they are related to" is the
-- requirement (programme, 2026-08-17); editing and deleting other people's accounts is a wider grant
-- that nobody has asked for, and the delegation ceiling now means whatever PILLAR_ADMIN holds is
-- also what a pillar admin can hand to someone else.
--
-- SUPER_ADMIN takes it too, because it is defined as the union of every permission that exists. A
-- permission it does not hold is one it can never assign, now that assignment is bounded by what the
-- grantor holds.
--
-- This does NOT finish the reconciliation. USER_WRITE, USER_DELETE, ORGANISATION_*, DOCUMENT_* and
-- NOTIFICATIONS_VIEW are still realm-only, so they cannot be granted through the RBAC screens; and
-- DASHBOARD_VIEW, CLAIMS_* and REGISTRATIONS_VERIFY are still catalogue-only, enforced nowhere.
-- Which side becomes authoritative is a design decision, not a migration (10.3).
--
-- Keycloak is NOT updated from here. RoleServiceImpl.syncToKeycloak mirrors a role when it is
-- created or updated through the API; a migration bypasses that, so deploy/keycloak/realm-export.json
-- carries the matching composite and a running realm is updated alongside. The two must agree or the
-- token will not carry what the catalogue says the role grants.

INSERT INTO permissions (id, name, description, resource, created_at, created_by, updated_at, updated_by) VALUES
    ('30000000-0000-0000-0000-000000000009', 'USER_READ',
     'Read user records. Which records is decided separately, by the caller''s visibility scope '
     '(ipie-user-service V9: pillar scope and organisation hierarchy).', 'USERS',
     now(), 'flyway-seed', now(), 'flyway-seed')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM roles r, permissions p
 WHERE r.name IN ('PILLAR_ADMIN', 'SUPER_ADMIN')
   AND p.name = 'USER_READ'
ON CONFLICT DO NOTHING;
