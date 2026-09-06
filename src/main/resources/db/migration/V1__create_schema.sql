-- Enables gen_random_uuid(), kept available for ad hoc use even though application-generated
-- entity ids are the primary id source (see UserJpaEntity).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
