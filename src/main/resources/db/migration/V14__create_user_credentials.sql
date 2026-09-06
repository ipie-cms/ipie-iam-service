-- ipie-iam-service becomes the credential authority (ARCHITECTURE_WORKING_PLAN.md, D1/D2): Keycloak issues
-- tokens and holds identity, roles and attributes, but never a password. Accounts are created in
-- Keycloak with no credentials at all; the hash lives here, and Keycloak asks this service whether
-- a submitted password is correct (via the ipie-keycloak-spi authenticators).
--
-- Keyed on keycloak_user_id rather than the ipie user id because the caller on the hot path is
-- Keycloak's own authenticator, which knows only its UserModel id. Joining through
-- ipie-user-service to translate an id would put a second service on the login path.

CREATE TABLE user_credentials (
    keycloak_user_id  UUID PRIMARY KEY,

    -- Argon2id, in the standard encoded form ($argon2id$v=19$m=...,t=...,p=...$salt$hash), so the
    -- salt and cost parameters travel with the hash. A password is never recoverable from this.
    password_hash     TEXT NOT NULL,

    -- Recorded explicitly rather than parsed back out of the hash. When the cost parameters are
    -- raised - and they will be, as hardware improves - this is what lets a rehash-on-next-login
    -- migration find the rows still on the old settings without re-parsing every hash.
    algorithm         VARCHAR(50) NOT NULL,

    updated_at        TIMESTAMPTZ NOT NULL
);

-- One-time tokens that authorise setting the *initial* password on an account provisioned without
-- credentials. Minted here (not in ipie-user-service) because this service owns credentials; the
-- token reaches the registrant through ipie-communication-service and never passes through
-- ipie-user-service at all.
CREATE TABLE credential_setup_tokens (
    -- The SHA-256 of the token, hex-encoded - never the token itself. A token is a bearer secret:
    -- whoever holds it can set the account's password. Storing it in clear would mean a database
    -- read, a backup, or a support query yields working ones. Compared by hashing the submitted
    -- value and looking that up, so this table is useless to anyone who reads it.
    --
    -- VARCHAR, not CHAR, despite the value always being exactly 64 hex characters. CHAR is
    -- blank-padded in Postgres, which turns an equality lookup into a padding question, and
    -- Hibernate reports the resulting bpchar as a schema mismatch against the entity's varchar
    -- mapping - the service refuses to start rather than run against a schema it cannot trust.
    token_hash        VARCHAR(64) PRIMARY KEY,

    keycloak_user_id  UUID NOT NULL,
    issued_at         TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,

    -- Single-use. Set when the token is spent; a non-null value here rejects any later attempt,
    -- which is what stops a link staying live in a mailbox for the rest of its TTL.
    consumed_at       TIMESTAMPTZ
);

-- No foreign key to user_credentials: the token is issued *before* any credential row exists -
-- that is the entire point of it - so a reference would be unsatisfiable at insert time.

-- Supports re-issuing a link ("send me another") by finding and invalidating this user's
-- outstanding tokens, and expiry sweeps by user.
CREATE INDEX idx_credential_setup_tokens_user ON credential_setup_tokens (keycloak_user_id);

-- Supports the retention sweep that deletes spent and expired rows. A consumed or expired token is
-- dead weight with a user id attached, and DPDP storage limitation says dead weight goes.
CREATE INDEX idx_credential_setup_tokens_expires_at ON credential_setup_tokens (expires_at);

-- ---------------------------------------------------------------------------------------------
-- Access control, because these two tables do NOT sit in a database of their own.
--
-- ipie-iam-service shares ipie_user_service with ipie-user-service by an explicit, documented
-- decision (ARCHITECTURE_WORKING_PLAN.md, D6) - the two keep separate Flyway timelines
-- (flyway_schema_history_iam) but one physical database. Everything else in this platform's design
-- says ipie-user-service must never be able to reach a credential: it owns the person, not the
-- account. In a shared database that is not true by construction, so it has to be true by grant.
--
-- Guarded, and a deliberate no-op in local development: there both services connect as `postgres`,
-- a superuser, which ignores grants entirely. The role separation is what makes this effective, and
-- that belongs to the deployment - see the plan's Stage 5. Running as superuser in a deployed
-- environment defeats this block silently, which is exactly why it says so here.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_user_service_app') THEN
        REVOKE ALL ON user_credentials FROM ipie_user_service_app;
        REVOKE ALL ON credential_setup_tokens FROM ipie_user_service_app;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_iam_service_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON user_credentials TO ipie_iam_service_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON credential_setup_tokens TO ipie_iam_service_app;
    END IF;
END
$$;
