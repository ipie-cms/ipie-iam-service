package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.NotFoundException;

public class RoleNotFoundException extends NotFoundException {

    public RoleNotFoundException(String roleName) {
        super(RoleErrorCode.ROLE_NOT_FOUND, "Role '" + roleName + "' was not found");
    }
}
