package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * The set-password link is unknown, has expired, or has already been used.
 *
 * <p>One exception for all three deliberately: telling a caller which it was would turn this
 * endpoint into an oracle for which tokens exist and which accounts have already completed setup.
 * The user-facing remedy is the same in every case - ask for a new link.
 *
 * <p>Extends {@link IpieException} directly, so {@code GlobalExceptionHandler} maps it to
 * <b>HTTP 422</b>: a submitted token is a credential, not an addressable resource, and 404 would
 * read as "no such endpoint".
 */
public class InvalidCredentialSetupTokenException extends IpieException {

    public InvalidCredentialSetupTokenException() {
        super(CredentialErrorCode.INVALID_CREDENTIAL_SETUP_TOKEN, "Password setup link is invalid or has expired");
    }
}
