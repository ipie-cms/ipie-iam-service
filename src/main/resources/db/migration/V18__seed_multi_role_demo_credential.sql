-- Credential for the multi-role demonstration account seeded in ipie-user-service V6.
--
-- Keycloak no longer validates passwords (ARCHITECTURE_WORKING_PLAN.md, D1) - it asks this service - so a
-- realm user without a row here cannot log in at all, and fails in a way that reads as a wrong
-- password rather than a missing credential. Same reasoning as V15, which seeded the rest.
--
-- The hash is real Argon2id, produced with the parameters PasswordHasher uses (m=19456, t=2, p=1,
-- 16-byte salt, 32-byte hash) and verified against its plaintext at generation time. It must not be
-- edited by hand: a malformed hash makes the account unloginnable and looks like a wrong password.
--
-- The password is `ipie-demo-pass`, the same one every demo account carries, and like the others it
-- predates the realm's 12-character policy. THIS FILE MUST NOT REACH PRODUCTION - it seeds a known
-- password for a known account, exactly as V15 does.

INSERT INTO user_credentials (keycloak_user_id, password_hash, algorithm, updated_at) VALUES
    ('20000000-0000-0000-0000-000000000012',
     '$argon2id$v=19$m=19456,t=2,p=1$Wfyrd1KniuTdvI3mcINBSw$yDi7egPOEeyoN91IWjJcwxYx3y5toUox47kw13sny8k',
     'argon2id-m19456-t2-p1', now())
ON CONFLICT (keycloak_user_id) DO NOTHING;  -- multirole.demo@ipie.gov.in
