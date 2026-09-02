package in.gov.ipie.service.iam.service;

import java.util.List;
import java.util.UUID;

import in.gov.ipie.service.iam.command.AssignRoleCommand;
import in.gov.ipie.service.iam.command.CreatePermissionCommand;
import in.gov.ipie.service.iam.command.CreateRoleCommand;
import in.gov.ipie.service.iam.command.RevokeRoleCommand;
import in.gov.ipie.service.iam.command.UpdateRoleCommand;
import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.domain.Role;

/**
 * Role/permission use cases. See {@link RoleServiceImpl} for the implementation - the interface
 * exists so callers depend on a contract rather than a concrete class.
 */
public interface RoleService {

    Role createRole(CreateRoleCommand command);

    /** Edits an existing role's description and permission set. The name is not editable. */
    Role updateRole(UpdateRoleCommand command);

    /**
     * Removes the role here and from Keycloak. Refused while any user still holds it - see {@code
     * RoleValidationAspect}.
     */
    void deleteRole(UUID roleId);

    List<Role> listRoles();

    /** The catalogue of permissions a role can be composed from. */
    List<Permission> listPermissions();

    /**
     * Adds an entry to the permission catalogue. SUPER_ADMIN only. Name uniqueness is validated in
     * {@code RoleValidationAspect}, consistent with {@link #createRole}.
     */
    Permission createPermission(CreatePermissionCommand command);

    /**
     * Assigns an existing role to a user - writes {@code user_roles} (this service's source of
     * truth) and additively syncs the realm-role mapping to Keycloak.
     */
    void assignRole(AssignRoleCommand command);

    /** Withdraws a role from a user, in {@code user_roles} and in Keycloak. */
    void revokeRole(RevokeRoleCommand command);

    /** An administrator's view of another user's roles, keyed on ipie-user-service's user id. */
    List<Role> getRolesForUser(UUID userId);

    /** Auto-assigns the default role when ipie-user-service publishes {@code USER_VERIFIED}. */
    void assignDefaultRole(UUID userId, UUID keycloakUserId, String defaultRoleName);

    /** Resolves the caller's own roles from the JWT {@code sub} claim. */
    List<Role> getRolesForKeycloakUser(UUID keycloakUserId);
}
