package in.gov.ipie.service.iam.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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

class RoleValidationAspectTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final RoleValidationAspect aspect =
            new RoleValidationAspect(roleRepository, permissionRepository, userRoleRepository);

    @Test
    void validateCreateRole_passes_whenRoleNameIsUniqueAndPermissionsExist() {
        when(permissionRepository.existsByName("CLAIMS_READ")).thenReturn(true);

        assertThatCode(() -> aspect.validateCreateRole(new CreateRoleCommand("CREDITOR", "IBC creditor", Set.of("CLAIMS_READ"))))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateRole_throwsConflict_whenRoleAlreadyExists() {
        when(roleRepository.existsByName("CREDITOR")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateRole(new CreateRoleCommand("CREDITOR", "desc", Set.of())))
                .isInstanceOf(RoleAlreadyExistsException.class);
    }

    @Test
    void validateCreateRole_throwsNotFound_whenPermissionUnknown() {
        when(roleRepository.existsByName("CREDITOR")).thenReturn(false);
        when(permissionRepository.existsByName("BOGUS")).thenReturn(false);

        assertThatThrownBy(() -> aspect.validateCreateRole(new CreateRoleCommand("CREDITOR", "desc", Set.of("BOGUS"))))
                .isInstanceOf(UnknownPermissionException.class);
    }

    @Test
    void validateCreatePermission_passes_whenTheNameIsFree() {
        when(permissionRepository.existsByName("CLAIMS_APPROVE")).thenReturn(false);

        assertThatCode(() -> aspect.validateCreatePermission(
                new CreatePermissionCommand("CLAIMS_APPROVE", "Approve claims", "CLAIMS")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreatePermission_throwsConflict_whenThePermissionAlreadyExists() {
        when(permissionRepository.existsByName("CLAIMS_READ")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreatePermission(
                new CreatePermissionCommand("CLAIMS_READ", "duplicate", "CLAIMS")))
                .isInstanceOf(PermissionAlreadyExistsException.class);
    }

    @Test
    void validateDeleteRole_throwsConflict_whenUsersStillHoldTheRole() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role(roleId, "CREDITOR", "d", Set.of(), null)));
        when(userRoleRepository.existsByRoleId(roleId)).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateDeleteRole(roleId)).isInstanceOf(RoleInUseException.class);
    }

    @Test
    void validateDeleteRole_passes_whenNobodyHoldsTheRole() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role(roleId, "CREDITOR", "d", Set.of(), null)));
        when(userRoleRepository.existsByRoleId(roleId)).thenReturn(false);

        assertThatCode(() -> aspect.validateDeleteRole(roleId)).doesNotThrowAnyException();
    }

    @Test
    void validateUpdateRole_throws_whenAPermissionIsNotInTheCatalogue() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role(roleId, "CREDITOR", "d", Set.of(), null)));
        when(permissionRepository.existsByName("MADE_UP")).thenReturn(false);

        assertThatThrownBy(() -> aspect.validateUpdateRole(new UpdateRoleCommand(roleId, "d", Set.of("MADE_UP"))))
                .isInstanceOf(UnknownPermissionException.class);
    }

    @Test
    void validateUpdateRole_throws_whenTheRoleDoesNotExist() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aspect.validateUpdateRole(new UpdateRoleCommand(roleId, "d", Set.of())))
                .isInstanceOf(RoleNotFoundException.class);
    }
}
