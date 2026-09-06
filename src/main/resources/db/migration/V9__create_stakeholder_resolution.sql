-- Read-optimised projection of ipie-user-service's authoritative stakeholder_links table
-- (ADR-001, "Placement of stakeholder_links Data and /resolve Endpoint") - kept current by
-- StakeholderResolutionEventConsumer as AccountLinked/AccountUnlinkedEvent arrive. Deliberately
-- minimal: only what /internal/stakeholder-links/resolve needs on the login hot path.
--
-- stakeholder_type is a plain VARCHAR here, not a CHECK-constrained enum like the source table's
-- - the two services' allowed values already drifted out of sync once this session (a type added
-- to the source without the constraint being updated), so this projection intentionally does not
-- maintain its own copy of the valid-values list.

CREATE TABLE stakeholder_resolution (
    id                          UUID PRIMARY KEY,
    stakeholder_type            VARCHAR(20) NOT NULL,
    external_stakeholder_id     VARCHAR(100) NOT NULL,
    ipie_user_id                UUID NOT NULL,
    keycloak_user_id            UUID NOT NULL,
    verified                    BOOLEAN NOT NULL DEFAULT true,
    synced_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_stakeholder_resolution_type_external_id
    ON stakeholder_resolution (stakeholder_type, external_stakeholder_id);
