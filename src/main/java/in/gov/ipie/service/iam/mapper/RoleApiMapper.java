package in.gov.ipie.service.iam.mapper;

import org.mapstruct.Mapper;

import in.gov.ipie.service.iam.dto.request.CreatePermissionRequest;
import in.gov.ipie.service.iam.dto.request.CreateRoleRequest;
import in.gov.ipie.service.iam.dto.response.PermissionResponse;
import in.gov.ipie.service.iam.dto.response.RoleResponse;
import in.gov.ipie.service.iam.command.CreatePermissionCommand;
import in.gov.ipie.service.iam.command.CreateRoleCommand;
import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.domain.Role;

@Mapper(componentModel = "spring")
public interface RoleApiMapper {

    CreateRoleCommand toCommand(CreateRoleRequest request);

    CreatePermissionCommand toCommand(CreatePermissionRequest request);

    RoleResponse toResponse(Role role);

    PermissionResponse toResponse(Permission permission);
}
