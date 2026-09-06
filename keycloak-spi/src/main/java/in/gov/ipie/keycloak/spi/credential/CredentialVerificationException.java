package in.gov.ipie.keycloak.spi.credential;

/**
 * The password check could not be performed - ipie-iam-service was unreachable, rejected the call,
 * or answered with something unparseable.
 *
 * <p>Deliberately distinct from "the password is wrong". A wrong password is a normal answer and is
 * reported as {@code valid: false} in a 200 body; this means <b>no answer was obtained</b>. The
 * authenticators must fail the login closed on it, never treat it as either outcome.
 */
class CredentialVerificationException extends RuntimeException {

    CredentialVerificationException(String message) {
        super(message);
    }

    CredentialVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
