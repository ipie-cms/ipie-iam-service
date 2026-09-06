package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.ConflictException;

/**
 * Raised when a role is deleted while users still hold it. A conflict rather than a bad request:
 * nothing about the request is wrong, it is the current state of the system that refuses it, and
 * the same call succeeds once the assignments are revoked.
 */
public class RoleInUseException extends ConflictException {

    public RoleInUseException(String roleName) {
        super(RoleErrorCode.ROLE_IN_USE, "Role '" + roleName + "' is still assigned to one or more users");
    }
}
