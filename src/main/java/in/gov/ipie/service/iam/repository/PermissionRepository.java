package in.gov.ipie.service.iam.repository;

import java.util.List;

import in.gov.ipie.service.iam.domain.Permission;

/**
 * Domain-owned port for the Permission catalogue. The application layer validates that a permission
 * name a role references exists, and reads the catalogue so an administrator composing a role is
 * offered only names that mean something to a {@code @RequiresPermission} check somewhere.
 *
 * <p>Permissions were seed-only until a SUPER_ADMIN-gated create path was added; migrations remain
 * the origin of every permission the code itself enforces. See {@link Permission} for why a
 * runtime-created permission grants nothing on its own.
 */
public interface PermissionRepository {

    boolean existsByName(String name);

    /** The full catalogue an administrator can compose a role from. */
    List<Permission> findAll();

    /** Persists a new permission and returns it with its assigned id. */
    Permission save(Permission permission);
}
