-- Takes case-data permissions off the qualification roles, and splits CLAIMS_WRITE into the two
-- different powers it was doing at once. Settled with the programme on 2026-08-16/17; the reasoning
-- is in ARCHITECTURE_WORKING_PLAN.md 10.1.
--
-- WHY A QUALIFICATION MUST NOT CARRY CLAIM ACCESS. INSOLVENCY_PROFESSIONAL says what a person *is* -
-- an IBBI registration that outlives every case and is scoped to none of them. Granting it
-- CLAIMS_READ therefore grants claim access on every case in the platform, including the ones where
-- the holder acts in no capacity at all. The same is true of CREDITOR. Nothing enforces these
-- permissions yet - there is no claims service - so this is a defect in the shape of the model
-- rather than a live disclosure, and it is cheap now and a rewrite once a service is built on it.
-- The permissions belong to capacities (IRP, RP, Liquidator, AR_CLASS, AR_ENTITY), which exist only
-- as case-scoped rows for a bounded interval, so there is no way to hold claim access except on a
-- named case for a named period.
--
-- CLAIMS_WRITE WAS TWO POWERS UNDER ONE NAME, and the split matters because one of them is held by
-- someone who is not yet a party:
--
--   CLAIMS_FILE    submit your own claim. Filing is the act that *creates* the creditor
--                  relationship, so gating it on already being a creditor of the case means nobody
--                  can ever file the first one.
--   CLAIMS_VERIFY  admit, reject and classify other people's claims - the resolution professional's
--                  power, and the act that decides whether a debt is financial or operational.
--
-- One permission covering both would have let anyone who may file also adjudicate.
--
-- CREDITOR IS LEFT IN THE CATALOGUE, deliberately, and stripped rather than removed. Creditor status
-- is a per-case, per-organisation, adjudicated relationship - derived from an admitted claim, not a
-- role a person holds - so the role has no future in this table. Removing it belongs with the case
-- model that replaces it, and deleting it today would orphan a seeded assignment while leaving
-- nothing able to express what it meant. The role is inert once its case permissions are gone.
--
-- SUPER_ADMIN TAKES THE NEW PERMISSIONS because it is defined as the union of every permission that
-- exists (RoleServiceImpl.grantToSuperAdmin). A permission it does not hold is one it can never
-- assign either, now that the delegation ceiling is enforced.

INSERT INTO permissions (id, name, description, resource, created_at, created_by, updated_at, updated_by) VALUES
    ('30000000-0000-0000-0000-000000000007', 'CLAIMS_FILE',
     'Submit a claim of your own against a case - the act that establishes the creditor relationship', 'CLAIMS',
     now(), 'flyway-seed', now(), 'flyway-seed'),
    ('30000000-0000-0000-0000-000000000008', 'CLAIMS_VERIFY',
     'Admit, reject and classify claims on a case as financial or operational - held on the case, never globally', 'CLAIMS',
     now(), 'flyway-seed', now(), 'flyway-seed')
ON CONFLICT (name) DO NOTHING;

-- The qualification roles keep DASHBOARD_VIEW and nothing case-scoped.
DELETE FROM role_permissions
 WHERE role_id IN (SELECT id FROM roles WHERE name IN ('INSOLVENCY_PROFESSIONAL', 'CREDITOR'))
   AND permission_id IN (SELECT id FROM permissions WHERE resource = 'CLAIMS');

-- CLAIMS_WRITE disappears entirely rather than lingering as a synonym for one of its halves; a name
-- that used to mean two things is the one a reader will guess wrong about.
DELETE FROM role_permissions WHERE permission_id = '30000000-0000-0000-0000-000000000003';
DELETE FROM permissions      WHERE id            = '30000000-0000-0000-0000-000000000003';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM roles r, permissions p
 WHERE r.name = 'SUPER_ADMIN'
   AND p.name IN ('CLAIMS_FILE', 'CLAIMS_VERIFY')
ON CONFLICT DO NOTHING;
