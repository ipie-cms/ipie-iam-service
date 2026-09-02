package in.gov.ipie.service.iam.service;

import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import in.gov.ipie.service.iam.command.CreatePermissionCommand;
import in.gov.ipie.service.iam.command.CreateRoleCommand;
import in.gov.ipie.service.iam.command.UpdateRoleCommand;
import in.gov.ipie.service.iam.domain.Role;
import in.gov.ipie.service.iam.exception.PermissionAlreadyExistsException;
import in.gov.ipie.service.iam.exception.RoleAlreadyExistsException;
import in.gov.ipie.service.iam.exception.RoleInUseException;
import in.gov.ipie.service.iam.exception.RoleNotFoundException;
import in.gov.ipie.service.iam.exception.UnknownPermissionException;
import in.gov.ipie.service.iam.repository.PermissionRepository;
import in.gov.ipie.service.iam.repository.RoleRepository;
import in.gov.ipie.service.iam.repository.UserRoleRepository;

/**
 * Role-name uniqueness, permission existence and delete-safety validation for {@link
 * RoleServiceImpl}, run before the method body via AspectJ pointcuts rather than inline {@code
 * if}/{@code throw} checks at the top of each service method.
 */
@Aspect
@Component
class RoleValidationAspect {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    RoleValidationAspect(
            RoleRepository roleRepository, PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Before("execution(* in.gov.ipie.service.iam.service.RoleServiceImpl.createRole(..)) && args(command)")
    void validateCreateRole(CreateRoleCommand command) {
        if (roleRepository.existsByName(command.name())) {
            throw new RoleAlreadyExistsException(command.name());
        }
        requireKnownPermissions(command.permissionNames());
    }

    @Before("execution(* in.gov.ipie.service.iam.service.RoleServiceImpl.createPermission(..)) && args(command)")
    void validateCreatePermission(CreatePermissionCommand command) {
        if (permissionRepository.existsByName(command.name())) {
            throw new PermissionAlreadyExistsException(command.name());
        }
    }

    @Before("execution(* in.gov.ipie.service.iam.service.RoleServiceImpl.updateRole(..)) && args(command)")
    void validateUpdateRole(UpdateRoleCommand command) {
        if (roleRepository.findById(command.roleId()).isEmpty()) {
            throw new RoleNotFoundException(command.roleId().toString());
        }
        requireKnownPermissions(command.permissionNames());
    }

    /**
     * A role still held by someone cannot be deleted. Deleting it would strip access from those
     * users on their next token refresh, with nothing in the request to say who was affected -
     * the administrator has to revoke the assignments first and see how many there are.
     */
    @Before("execution(* in.gov.ipie.service.iam.service.RoleServiceImpl.deleteRole(..)) && args(roleId)")
    void validateDeleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId.toString()));
        if (userRoleRepository.existsByRoleId(roleId)) {
            throw new RoleInUseException(role.getName());
        }
    }

    private void requireKnownPermissions(Set<String> permissionNames) {
        for (String permissionName : permissionNames) {
            if (!permissionRepository.existsByName(permissionName)) {
                throw new UnknownPermissionException(permissionName);
            }
        }
    }
}
