package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * Change-password was called with the wrong current password.
 *
 * <p>Requiring the current password is what stops a stolen session from becoming a permanent
 * account takeover: an attacker holding a token can act as the user until it expires, but cannot
 * lock the real owner out by changing the password without also knowing it.
 */
public class InvalidCurrentPasswordException extends IpieException {

    public InvalidCurrentPasswordException() {
        super(CredentialErrorCode.INVALID_CURRENT_PASSWORD, "The current password is incorrect");
    }
}
