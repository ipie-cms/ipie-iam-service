package in.gov.ipie.service.iam.repository;

import java.util.List;
import java.util.UUID;

/**
 * Domain-owned port for the {@code user_roles} assignment table. {@code userId} is
 * ipie-user-service's own user id, not a foreign key here - separate services, separate
 * databases.
 */
public interface UserRoleRepository {

    /** No-op if the assignment already exists (idempotent re-assignment, e.g. a retried request). */
    void assign(UUID userId, UUID keycloakUserId, UUID roleId, String assignedBy);

    /** The only query path a dashboard load actually has - the JWT's own {@code sub} claim, not ipie-user-service's internal id. */
    List<UUID> findRoleIdsByKeycloakUserId(UUID keycloakUserId);

    /**
     * The administration counterpart of {@link #findRoleIdsByKeycloakUserId} - an administrator
     * managing someone else's roles has that user's ipie-user-service id to hand, not their
     * Keycloak subject.
     */
    List<UUID> findRoleIdsByUserId(UUID userId);

    /** No-op if the assignment does not exist, so a repeated revoke is not an error. */
    void revoke(UUID userId, UUID roleId);

    /**
     * Whether any user still holds this role - a role with live assignments must not be deleted
     * out from under them.
     */
    boolean existsByRoleId(UUID roleId);
}
