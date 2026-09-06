-- Renames STAKEHOLDER_ADMIN to PILLAR_ADMIN, because "stakeholder" was doing two jobs and the
-- programme has now settled which one it keeps (user, 2026-08-17).
--
-- IBBI, NCLT, NCLAT, MCA and NeSL are the **pillars** of iPIE - the institutions the platform is
-- built around and federates identity with. A stakeholder is something else: a user of iPIE under
-- the umbrella of a pillar, an IP or an entity, related to an IP, a financial or operational
-- creditor, or a corporate debtor. The FRS treats those as a domain of their own, with a separate
-- stakeholder-management microservice to come, so the word is not free to mean "one of the five
-- institutions" as well.
--
-- The old name said the opposite of what it meant. STAKEHOLDER_ADMIN administers a pillar, not
-- stakeholders, while STAKEHOLDER - the base grant every verified user holds - genuinely does mean
-- a stakeholder. Two roles one underscore apart pointed at unrelated tiers, and the reading that
-- looked obvious was the wrong one.
--
-- ONLY THE NAME MOVES. role_permissions and user_roles both key on roles.id, so every grant and
-- every assignment follows the rename without being touched. The realm role in Keycloak carries the
-- same name and is renamed alongside this (deploy/keycloak/realm-export.json); the two must agree,
-- because RoleServiceImpl mirrors a role into the realm by name and a mismatch would silently
-- create a second role rather than fail.
--
-- IDEMPOTENT. The WHERE clause makes a second run a no-op, which matters because a running
-- environment is repaired by hand before this migration reaches it.

UPDATE roles
   SET name = 'PILLAR_ADMIN',
       description = 'Administers one pillar (IBBI/NCLT/NCLAT/MCA/NeSL) and its users - verifies pending registrations and manages roles. Below SUPER_ADMIN.',
       updated_at = now(),
       updated_by = 'flyway-seed'
 WHERE name = 'STAKEHOLDER_ADMIN';

-- SUPER_ADMIN's description named the old role, and a stale cross-reference in the one place an
-- administrator reads to understand the hierarchy is worse than no description. The "(IBBI/MCA)"
-- parenthetical is left alone deliberately: whether MCA and IBBI operate the platform, participate
-- as pillars, or do both from separate accounts is still open (10.1), and this migration should not
-- settle it by editing a sentence.
UPDATE roles
   SET description = 'Platform operator - top of the role hierarchy, above PILLAR_ADMIN. Holds every permission by definition.',
       updated_at = now(),
       updated_by = 'flyway-seed'
 WHERE name = 'SUPER_ADMIN';
