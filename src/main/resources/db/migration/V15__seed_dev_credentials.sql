-- Credentials for the demo/test accounts seeded in deploy/keycloak/realm-export.json, consistent
-- with V7__seed_dummy_rbac_data.sql and ipie-user-service's V9__seed_dummy_registrations.sql.
--
-- WHY THIS IS NEEDED AT ALL: Keycloak no longer validates passwords (ARCHITECTURE_WORKING_PLAN.md, D1) -
-- it asks this service. The realm export still gives these accounts Keycloak passwords, but nothing
-- reads them any more. Without the rows below, every seeded account silently stops being able to log
-- in the moment the ipie-keycloak-spi authenticators go live, including `testuser`, which the jmeter
-- plans and the standards doc both depend on.
--
-- THE HASHES ARE REAL Argon2id, produced by the same encoder and the same parameters as
-- PasswordHasher (m=19456, t=2, p=1, salt 16, hash 32) and verified against their plaintext at
-- generation time. They are not hand-written and must not be edited by hand - a malformed hash makes
-- the account unloginnable in a way that looks like a wrong password.
--
-- THE PASSWORDS DELIBERATELY DO NOT MEET THE REALM'S 12-CHARACTER POLICY. That is not an oversight:
-- a password policy applies when a password is *set*, and these predate it - the same grandfathering
-- any real deployment gets when a policy is tightened. It is also why the realm import fails if the
-- policy and these users are applied in one step. Treat them as what they are: demo credentials that
-- should be forced to reset before anything resembling production.
--
-- THIS FILE MUST NOT REACH PRODUCTION. It seeds known passwords for known accounts. It sits in the
-- same category as V7/V9, and the platform still has no mechanism separating seed migrations from
-- schema ones - see ARCHITECTURE_WORKING_PLAN.md's open questions.
--
-- The user ids are Keycloak user ids, fixed in realm-export.json so a re-import reproduces them.
-- testuser/readonlyuser were given explicit ids on 2026-08-11 for exactly this reason; before that
-- Keycloak generated a fresh id per import and any seed keyed on them broke silently.
INSERT INTO user_credentials (keycloak_user_id, password_hash, algorithm, updated_at) VALUES
    ('20000000-0000-0000-0000-000000000010', '$argon2id$v=19$m=19456,t=2,p=1$bq6yTrWt6yRFUCzII5rl3Q$PXOEaVtZKitD6Bu6gkeeJOQA14nIVe3y6p0hKeJNpSg', 'argon2id-m19456-t2-p1', now()),  -- testuser
    ('20000000-0000-0000-0000-000000000011', '$argon2id$v=19$m=19456,t=2,p=1$F/+s8FO1HPtQYSoBfUd/tA$IfR1Jdj0VVL6+0A1vTNjo7jWSA6wCQeRyUPvzPO0bBg', 'argon2id-m19456-t2-p1', now()),  -- readonlyuser
    ('20000000-0000-0000-0000-000000000001', '$argon2id$v=19$m=19456,t=2,p=1$mqZy2QPCAn9bpeDptL7toA$52vbxisHdfvpw2VkXITJOBahf3iKzNDM1Z1qThw/4kI', 'argon2id-m19456-t2-p1', now()),  -- admin@ipie.gov.in
    ('20000000-0000-0000-0000-000000000005', '$argon2id$v=19$m=19456,t=2,p=1$I7rfzyvRVeP7uW6hbC0S7w$NFQannt5kby9ZjDLEgitAzxmmLZ0+yI+5cfk40/+LRA', 'argon2id-m19456-t2-p1', now()),  -- superadmin@ipie.gov.in
    ('20000000-0000-0000-0000-000000000002', '$argon2id$v=19$m=19456,t=2,p=1$I7eUIek81Rn8yckZRGntPg$sM/TWqVXHLQ78RD59padBLM2Fh61B2vrErGmI2XUJAI', 'argon2id-m19456-t2-p1', now()),  -- creditor.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000003', '$argon2id$v=19$m=19456,t=2,p=1$mNEUKuktTNyksWkTsdB+Hg$5K61wOECGsX7yed2JZs2lG1tund9pODttr8NK6TY0tg', 'argon2id-m19456-t2-p1', now()),  -- professional.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000006', '$argon2id$v=19$m=19456,t=2,p=1$s0pfqzVGwO28ACxmt1myJw$/n7REKB2PqQ3+nY1Ufpb2Qd60GAlmpc+GDGeY5Vgb5k', 'argon2id-m19456-t2-p1', now()),  -- rv.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000007', '$argon2id$v=19$m=19456,t=2,p=1$qU8z+/PbB/FGJG+mLqcAGw$hW1ZoGTFXbsnXw+J+iqkG65TMc8xe4fQePoOl3QgB9Y', 'argon2id-m19456-t2-p1', now()),  -- legalrep.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000008', '$argon2id$v=19$m=19456,t=2,p=1$IzNS4JZ1G8qvcLpFH4XVIA$aT688y+CC8odJMVy0luDx4OCRb20UQGJh3xtHt936ns', 'argon2id-m19456-t2-p1', now()),  -- authorizedrep.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000009', '$argon2id$v=19$m=19456,t=2,p=1$Eu9+g0UVcBt/foG2gRKUQw$KADYKcrygd9ziqR3548gfI01v66hKXkucUfo2n4QrH0', 'argon2id-m19456-t2-p1', now()),  -- ibbi.official.demo@ipie.gov.in
    ('20000000-0000-0000-0000-000000000004', '$argon2id$v=19$m=19456,t=2,p=1$jG7MDGFrONeommqafQ4cRQ$CSgyAGHE5wWLZ/zfYkyAoe7rDgDkCZdsvEP0QSZdZGE', 'argon2id-m19456-t2-p1', now())
ON CONFLICT (keycloak_user_id) DO NOTHING;  -- pending.verification@ipie.gov.in
