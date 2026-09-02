package in.gov.ipie.service.iam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.common.security.context.CurrentUser;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.keycloak.admin.KeycloakUserManagementClient;
import in.gov.ipie.service.iam.command.AssignRoleCommand;
import in.gov.ipie.service.iam.command.CreatePermissionCommand;
import in.gov.ipie.service.iam.command.CreateRoleCommand;
import in.gov.ipie.service.iam.command.RevokeRoleCommand;
import in.gov.ipie.service.iam.command.UpdateRoleCommand;
import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.domain.Role;
import in.gov.ipie.service.iam.exception.RoleNotFoundException;
import in.gov.ipie.service.iam.repository.PermissionRepository;
import in.gov.ipie.service.iam.repository.RoleRepository;
import in.gov.ipie.service.iam.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

/** Role-name/permission-existence validation is done by {@code RoleValidationAspect} - see {@code RoleValidationAspectTest}, not here. */
class RoleServiceImplTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final KeycloakUserManagementClient keycloakUserManagementClient = mock(KeycloakUserManagementClient.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(
                roleRepository, userRoleRepository, permissionRepository, keycloakUserManagementClient,
                currentUserProvider);
        when(currentUserProvider.current()).thenReturn(Optional.of(new CurrentUser("admin-sub", "admin", Set.of())));
        when(roleRepository.save(any())).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            AuditMetadata auditMetadata = new AuditMetadata(null, "system", null, "system", 0, true, null, null);
            return new Role(UUID.randomUUID(), role.getName(), role.getDescription(), role.getPermissionNames(), auditMetadata);
        });
    }

    @Test
    void createRole_savesAndSyncsToKeycloak() {
        Role created = roleService.createRole(new CreateRoleCommand("CREDITOR", "IBC creditor", Set.of("CLAIMS_READ")));

        assertThat(created.getName()).isEqualTo("CREDITOR");
        verify(keycloakUserManagementClient).ensureRealmRole("CREDITOR", "IBC creditor");
    }

    /**
     * The absence of a Keycloak call is the point of the assertion, not an omission: a permission
     * becomes a realm role when a role granting it is created or updated, so mirroring it here
     * would leave an unattached realm role nothing composites and nobody holds.
     */
    @Test
    void createPermission_savesWithoutTouchingKeycloak() {
        when(permissionRepository.save(any())).thenAnswer(invocation -> {
            Permission permission = invocation.getArgument(0);
            return new Permission(UUID.randomUUID(), permission.name(), permission.description(), permission.resource());
        });

        Permission created = roleService.createPermission(
                new CreatePermissionCommand("CLAIMS_APPROVE", "Approve claims", "CLAIMS"));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("CLAIMS_APPROVE");
        assertThat(created.resource()).isEqualTo("CLAIMS");
        verifyNoInteractions(keycloakUserManagementClient);
    }

    @Test
    void assignRole_writesAssignmentAndSyncsToKeycloak() {
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        Role role = new Role(roleId, "STAKEHOLDER", "default role", Set.of(), null);
        when(roleRepository.findByName("STAKEHOLDER")).thenReturn(Optional.of(role));

        roleService.assignRole(new AssignRoleCommand(userId, keycloakUserId, "STAKEHOLDER", null));

        verify(userRoleRepository).assign(userId, keycloakUserId, roleId, "admin");
        verify(keycloakUserManagementClient).assignRealmRoles(keycloakUserId, List.of("STAKEHOLDER"));
    }

    @Test
    void assignRole_isRefused_whenTheRoleGrantsMoreThanTheGrantorHolds() {
        // The escalation this guard exists for: a pillar admin holds ROLES_MANAGE and nothing above
        // it, and before the ceiling could hand SUPER_ADMIN to anyone, including themselves.
        UUID grantorId = UUID.randomUUID();
        when(currentUserProvider.current())
                .thenReturn(Optional.of(new CurrentUser(grantorId.toString(), "pillar-admin", Set.of("ROLES_MANAGE"))));
        UUID pillarAdminRoleId = UUID.randomUUID();
        when(userRoleRepository.findRoleIdsByKeycloakUserId(grantorId)).thenReturn(List.of(pillarAdminRoleId));
        when(roleRepository.findById(pillarAdminRoleId)).thenReturn(
                Optional.of(new Role(pillarAdminRoleId, "PILLAR_ADMIN", "pillar", Set.of("ROLES_MANAGE"), null)));
        Role superAdmin = new Role(UUID.randomUUID(), "SUPER_ADMIN", "everything", Set.of("ROLES_MANAGE", "RBAC_DEFINE"), null);
        when(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> roleService.assignRole(
                new AssignRoleCommand(UUID.randomUUID(), UUID.randomUUID(), "SUPER_ADMIN", null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("RBAC_DEFINE");

        verify(userRoleRepository, never()).assign(any(), any(), any(), any());
    }

    @Test
    void assignRole_isAllowed_whenTheGrantorHoldsEveryPermissionTheRoleGrants() {
        UUID grantorId = UUID.randomUUID();
        when(currentUserProvider.current())
                .thenReturn(Optional.of(new CurrentUser(grantorId.toString(), "super", Set.of())));
        UUID superRoleId = UUID.randomUUID();
        when(userRoleRepository.findRoleIdsByKeycloakUserId(grantorId)).thenReturn(List.of(superRoleId));
        when(roleRepository.findById(superRoleId)).thenReturn(
                Optional.of(new Role(superRoleId, "SUPER_ADMIN", "everything", Set.of("ROLES_MANAGE", "RBAC_DEFINE"), null)));
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        when(roleRepository.findByName("SUPER_ADMIN"))
                .thenReturn(Optional.of(new Role(roleId, "SUPER_ADMIN", "everything", Set.of("ROLES_MANAGE", "RBAC_DEFINE"), null)));

        roleService.assignRole(new AssignRoleCommand(userId, keycloakUserId, "SUPER_ADMIN", null));

        verify(userRoleRepository).assign(userId, keycloakUserId, roleId, "super");
    }

    @Test
    void assignRole_skipsTheCeiling_whenThereIsNoAuthenticatedCaller() {
        // assignDefaultRole reaches this from the user-verified event consumer, where there is no
        // security context. Applying the ceiling there would fail every registration.
        when(currentUserProvider.current()).thenReturn(Optional.empty());
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        when(roleRepository.findByName("STAKEHOLDER"))
                .thenReturn(Optional.of(new Role(roleId, "STAKEHOLDER", "default", Set.of("DASHBOARD_VIEW"), null)));

        roleService.assignRole(new AssignRoleCommand(userId, keycloakUserId, "STAKEHOLDER", null));

        verify(userRoleRepository).assign(userId, keycloakUserId, roleId, "system");
    }

    @Test
    void createPermission_widensSuperAdminSoItStillHoldsEverything() {
        Role superAdmin = new Role(UUID.randomUUID(), "SUPER_ADMIN", "everything", Set.of("ROLES_MANAGE"), null);
        when(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin));
        when(permissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        roleService.createPermission(new CreatePermissionCommand("CASES_READ", "Read cases", "CASES"));

        ArgumentCaptor<Role> saved = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).update(saved.capture());
        assertThat(saved.getValue().getPermissionNames()).contains("ROLES_MANAGE", "CASES_READ");
        verify(keycloakUserManagementClient).syncRealmRoleComposites(eq("SUPER_ADMIN"), argThat(p -> p.contains("CASES_READ")));
    }

    @Test
    void assignRole_throwsNotFound_whenRoleNameUnknown() {
        when(roleRepository.findByName("BOGUS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignRole(new AssignRoleCommand(UUID.randomUUID(), UUID.randomUUID(), "BOGUS", null)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void getRolesForKeycloakUser_returnsAssignedRoles() {
        UUID keycloakUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role role = new Role(roleId, "STAKEHOLDER", "default role", Set.of("DASHBOARD_VIEW"), null);
        when(userRoleRepository.findRoleIdsByKeycloakUserId(keycloakUserId)).thenReturn(List.of(roleId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        List<Role> roles = roleService.getRolesForKeycloakUser(keycloakUserId);

        assertThat(roles).containsExactly(role);
    }

    @Test
    void createRole_makesTheRealmRoleCompositeOfItsPermissions() {
        // Without the composite step the realm role exists but grants nothing - a token issued for
        // a user holding it carries no permissions, while the role still looks correct in Keycloak.
        roleService.createRole(new CreateRoleCommand("CREDITOR", "IBC creditor", Set.of("CLAIMS_READ")));

        verify(keycloakUserManagementClient).ensureRealmRole("CLAIMS_READ", null);
        verify(keycloakUserManagementClient).syncRealmRoleComposites("CREDITOR", Set.of("CLAIMS_READ"));
    }

    @Test
    void updateRole_replacesPermissionsAndResyncsComposites() {
        UUID roleId = UUID.randomUUID();
        Role existing = new Role(roleId, "CREDITOR", "old", Set.of("CLAIMS_READ"), null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existing));
        when(roleRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Role updated = roleService.updateRole(new UpdateRoleCommand(roleId, "new", Set.of("CLAIMS_WRITE")));

        assertThat(updated.getDescription()).isEqualTo("new");
        assertThat(updated.getPermissionNames()).containsExactly("CLAIMS_WRITE");
        verify(keycloakUserManagementClient).syncRealmRoleComposites("CREDITOR", Set.of("CLAIMS_WRITE"));
    }

    @Test
    void deleteRole_removesTheRealmRoleTooSoTheTwoDoNotDrift() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role(roleId, "CREDITOR", "d", Set.of(), null)));

        roleService.deleteRole(roleId);

        verify(roleRepository).deleteById(roleId);
        verify(keycloakUserManagementClient).deleteRealmRole("CREDITOR");
    }

    @Test
    void revokeRole_removesTheAssignmentAndTheKeycloakMapping() {
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        when(roleRepository.findByName("CREDITOR")).thenReturn(Optional.of(new Role(roleId, "CREDITOR", "d", Set.of(), null)));

        roleService.revokeRole(new RevokeRoleCommand(userId, keycloakUserId, "CREDITOR", "left the panel"));

        verify(userRoleRepository).revoke(userId, roleId);
        verify(keycloakUserManagementClient).removeRealmRoles(keycloakUserId, List.of("CREDITOR"));
    }

    @Test
    void revokeRole_throws_whenTheRoleDoesNotExist() {
        when(roleRepository.findByName("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.revokeRole(
                new RevokeRoleCommand(UUID.randomUUID(), UUID.randomUUID(), "GHOST", null)))
                .isInstanceOf(RoleNotFoundException.class);
    }
}
