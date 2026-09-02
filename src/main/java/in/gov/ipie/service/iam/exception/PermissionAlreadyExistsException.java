package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.ConflictException;

public class PermissionAlreadyExistsException extends ConflictException {

    public PermissionAlreadyExistsException(String permissionName) {
        super(RoleErrorCode.PERMISSION_ALREADY_EXISTS, "Permission '" + permissionName + "' already exists");
    }
}
