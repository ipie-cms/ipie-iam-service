package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

public enum RoleErrorCode implements ErrorCode {
    ROLE_NOT_FOUND,
    ROLE_ALREADY_EXISTS,
    ROLE_IN_USE,
    UNKNOWN_PERMISSION,
    PERMISSION_ALREADY_EXISTS;

    @Override
    public String code() {
        return name();
    }
}
