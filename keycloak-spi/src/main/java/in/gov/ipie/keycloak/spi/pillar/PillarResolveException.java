package in.gov.ipie.keycloak.spi.pillar;

/** The resolve call to ipie-user-service failed - never rethrown to the browser directly, just triggers a login rejection. */
class PillarResolveException extends RuntimeException {

    PillarResolveException(String message) {
        super(message);
    }

    PillarResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
