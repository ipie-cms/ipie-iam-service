package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.ConflictException;

public class RoleAlreadyExistsException extends ConflictException {

    public RoleAlreadyExistsException(String roleName) {
        super(RoleErrorCode.ROLE_ALREADY_EXISTS, "Role '" + roleName + "' already exists");
    }
}
