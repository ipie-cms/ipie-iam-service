package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * A permission name submitted in a request body does not resolve to a known permission.
 *
 * <p>Extends {@link IpieException} directly rather than {@code NotFoundException}, so
 * {@code GlobalExceptionHandler}'s catch-all maps it to <b>HTTP 422</b>. The permission name is a
 * value inside the payload, never an addressed resource - a {@code POST /api/v1/roles} carrying one
 * bad name is a rejected submission, not a missing endpoint, and 404 was indistinguishable from a
 * wrong URL to the caller. Contrast {@link RoleNotFoundException}, which stays a 404 because it
 * genuinely addresses a role by id in the path.
 */
public class UnknownPermissionException extends IpieException {

    public UnknownPermissionException(String permissionName) {
        super(RoleErrorCode.UNKNOWN_PERMISSION, "Permission '" + permissionName + "' does not exist");
    }
}
