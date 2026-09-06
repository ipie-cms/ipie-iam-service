-- Renames the login-path projection from "stakeholder" to "pillar", the iam half of the vocabulary
-- split settled on 2026-08-17 (ARCHITECTURE_WORKING_PLAN.md 10.1). The user-service half is its V8.
--
-- This table is a read-optimised projection of one fact: which iPIE account an identity at IBBI,
-- NCLT, NCLAT, MCA or NeSL belongs to. Those five are the **pillars**; a stakeholder is a user of
-- iPIE under a pillar, an IP/IPE or an entity, and gets its own service in due course. The
-- projection is consulted on the login path by the Keycloak SPI through
-- /internal/pillar-links/resolve, so the name appears in an inter-service contract as well as in
-- the schema - both move together in this change.
--
-- Renaming the table does not rename its indexes or constraints, so those are renamed explicitly;
-- grants are held against the OID and survive untouched. Idempotent, because a running environment
-- is repaired by hand before this migration reaches it.

DO $$
BEGIN
    IF to_regclass('public.stakeholder_resolution') IS NOT NULL THEN
        ALTER TABLE stakeholder_resolution RENAME TO pillar_resolution;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_name = 'pillar_resolution' AND column_name = 'stakeholder_type') THEN
        ALTER TABLE pillar_resolution RENAME COLUMN stakeholder_type TO pillar_type;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_name = 'pillar_resolution' AND column_name = 'external_stakeholder_id') THEN
        ALTER TABLE pillar_resolution RENAME COLUMN external_stakeholder_id TO external_pillar_id;
    END IF;
END $$;

DO $$
DECLARE
    obj RECORD;
BEGIN
    FOR obj IN SELECT indexname AS n FROM pg_indexes
                WHERE schemaname = 'public' AND indexname LIKE '%stakeholder_resolution%'
    LOOP
        EXECUTE format('ALTER INDEX %I RENAME TO %I', obj.n, replace(obj.n, 'stakeholder_resolution', 'pillar_resolution'));
    END LOOP;

    FOR obj IN SELECT c.conname AS n FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid
                WHERE c.conname LIKE '%stakeholder%' AND t.relname = 'pillar_resolution'
    LOOP
        EXECUTE format('ALTER TABLE pillar_resolution RENAME CONSTRAINT %I TO %I',
                       obj.n, replace(replace(replace(obj.n, 'external_stakeholder_id', 'external_pillar_id'), 'stakeholder_resolution', 'pillar_resolution'), 'stakeholder_type', 'pillar_type'));
    END LOOP;
END $$;
