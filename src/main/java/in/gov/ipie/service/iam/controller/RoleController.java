package in.gov.ipie.service.iam.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.iam.permission.IamPermissions;
import in.gov.ipie.service.iam.mapper.RoleApiMapper;
import in.gov.ipie.service.iam.dto.request.AssignRoleRequest;
import in.gov.ipie.service.iam.dto.request.CreatePermissionRequest;
import in.gov.ipie.service.iam.dto.request.CreateRoleRequest;
import in.gov.ipie.service.iam.dto.request.RevokeRoleRequest;
import in.gov.ipie.service.iam.dto.request.UpdateRoleRequest;
import in.gov.ipie.service.iam.dto.response.PermissionResponse;
import in.gov.ipie.service.iam.dto.response.RoleResponse;
import in.gov.ipie.service.iam.command.AssignRoleCommand;
import in.gov.ipie.service.iam.command.RevokeRoleCommand;
import in.gov.ipie.service.iam.command.UpdateRoleCommand;
import in.gov.ipie.service.iam.service.RoleService;
import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.domain.Role;

/** Thin HTTP layer over {@link RoleService} - business rules live there and in the domain model. */
@RestController
public class RoleController {

    private final RoleService roleService;
    private final RoleApiMapper roleApiMapper;
    private final CurrentUserProvider currentUserProvider;

    public RoleController(RoleService roleService, RoleApiMapper roleApiMapper, CurrentUserProvider currentUserProvider) {
        this.roleService = roleService;
        this.roleApiMapper = roleApiMapper;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Defining the catalogue is {@link IamPermissions#RBAC_DEFINE}, not {@link
     * IamPermissions#ROLES_MANAGE}: composing a role decides what a capability *is*, while
     * assigning one hands out a capability that already exists. PILLAR_ADMIN does the second
     * and not the first.
     */
    @PostMapping("/api/v1/roles")
    @RequiresPermission(IamPermissions.RBAC_DEFINE)
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role created = roleService.createRole(roleApiMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(roleApiMapper.toResponse(created));
    }

    @PutMapping("/api/v1/roles/{roleId}")
    @RequiresPermission(IamPermissions.RBAC_DEFINE)
    public RoleResponse updateRole(@PathVariable UUID roleId, @Valid @RequestBody UpdateRoleRequest request) {
        Role updated = roleService.updateRole(
                new UpdateRoleCommand(roleId, request.description(), request.permissionNames()));
        return roleApiMapper.toResponse(updated);
    }

    @DeleteMapping("/api/v1/roles/{roleId}")
    @RequiresPermission(IamPermissions.RBAC_DEFINE)
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/roles")
    public List<RoleResponse> listRoles() {
        return roleService.listRoles().stream().map(roleApiMapper::toResponse).toList();
    }

    /**
     * The catalogue an administrator composes roles from. Readable by any authenticated caller for
     * the same reason {@code listRoles} is: it is the vocabulary of the system, not a secret, and
     * every write that acts on it is separately guarded.
     */
    @GetMapping("/api/v1/permissions")
    public List<PermissionResponse> listPermissions() {
        return roleService.listPermissions().stream().map(roleApiMapper::toResponse).toList();
    }

    /**
     * Adds an entry to that catalogue. SUPER_ADMIN only, via {@link IamPermissions#RBAC_DEFINE}.
     *
     * <p>Worth knowing before using it: a permission created here grants nothing by itself. It
     * becomes real when some service names it in a {@code @RequiresPermission} and when a role
     * granting it is assigned to someone - see {@code Permission}'s Javadoc. This endpoint prepares
     * the vocabulary; it does not extend anyone's access on its own.
     */
    @PostMapping("/api/v1/permissions")
    @RequiresPermission(IamPermissions.RBAC_DEFINE)
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission created = roleService.createPermission(roleApiMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(roleApiMapper.toResponse(created));
    }

    @PostMapping("/api/v1/users/{userId}/roles")
    @RequiresPermission(IamPermissions.ROLES_MANAGE)
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request) {
        roleService.assignRole(new AssignRoleCommand(userId, request.keycloakUserId(), request.roleName(), request.comment()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/users/{userId}/roles")
    @RequiresPermission(IamPermissions.ROLES_MANAGE)
    public ResponseEntity<Void> revokeRole(@PathVariable UUID userId, @Valid @RequestBody RevokeRoleRequest request) {
        roleService.revokeRole(
                new RevokeRoleCommand(userId, request.keycloakUserId(), request.roleName(), request.comment()));
        return ResponseEntity.noContent().build();
    }

    /**
     * An administrator's view of another user's roles. Guarded, unlike {@code /users/me/roles}
     * below, because the caller is naming someone else.
     */
    @GetMapping("/api/v1/users/{userId}/roles")
    @RequiresPermission(IamPermissions.ROLES_MANAGE)
    public List<RoleResponse> getRolesForUser(@PathVariable UUID userId) {
        return roleService.getRolesForUser(userId).stream().map(roleApiMapper::toResponse).toList();
    }

    /**
     * Called by ipie-web's dashboard to render role-based content once a user is VERIFIED -
     * always the caller's own roles (resolved from the JWT {@code sub} claim), never an arbitrary
     * user id, so no separate authorization check is needed here.
     */
    @GetMapping("/api/v1/users/me/roles")
    public List<RoleResponse> getMyRoles() {
        UUID keycloakUserId = UUID.fromString(currentUserProvider.currentOrThrow().userId());
        return roleService.getRolesForKeycloakUser(keycloakUserId).stream().map(roleApiMapper::toResponse).toList();
    }
}
