-- Grants ipie-iam-service's runtime role exactly what it needs, and nothing else.
-- ARCHITECTURE_WORKING_PLAN.md Stage 5, item 1. Mirrors ipie-user-service's V30 - see that file for why the
-- table list is written out rather than expressed as "all tables in this schema".
--
-- The credential tables are in this list and in no other service's: iam is the credential authority
-- (D2), and V14 additionally REVOKEs them from ipie_user_service_app so the boundary survives a
-- future blanket grant made somewhere else.
--
-- Roles are created by the deployment (ipie-platform-mca/deploy/postgres/roles/01-create-roles.sql);
-- this migration is a no-op without them.

DO $$
DECLARE
    owned_tables text[] := ARRAY[
        'user_credentials',
        'credential_setup_tokens',
        'roles',
        'permissions',
        'role_permissions',
        'user_roles',
        'stakeholder_resolution',
        'iam_audit_trail',
        'iam_outbox_events',
        'iam_processed_events'
        -- flyway_schema_history_iam is deliberately absent - owner-only, as in V30.
    ];
    target text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_iam_service_app') THEN
        RAISE NOTICE 'ipie_iam_service_app does not exist - skipping grants (local development)';
        RETURN;
    END IF;

    GRANT USAGE ON SCHEMA public TO ipie_iam_service_app;

    FOREACH target IN ARRAY owned_tables LOOP
        IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = target) THEN
            EXECUTE format(
                'GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO ipie_iam_service_app', target);
        ELSE
            RAISE WARNING 'V17: table % is listed but does not exist - grant skipped', target;
        END IF;
    END LOOP;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_iam_service_owner') THEN
        ALTER DEFAULT PRIVILEGES FOR ROLE ipie_iam_service_owner IN SCHEMA public
            GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ipie_iam_service_app;
    END IF;
END
$$;
