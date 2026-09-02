package in.gov.ipie.service.iam.dto.response;

/**
 * The answer to a login-path password check.
 *
 * <p>A body with a boolean rather than a 200/401 distinction: the caller is Keycloak's authenticator,
 * for which "the password is wrong" is a normal answer, not a transport failure. Keeping it in the
 * body leaves non-200 to mean what it should - the check could not be performed - so the
 * authenticator can tell "wrong password" from "iam is unreachable" and fail closed on the second.
 */
public record VerifyCredentialResponse(boolean valid) {
}
