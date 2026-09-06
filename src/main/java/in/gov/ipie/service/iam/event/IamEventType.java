package in.gov.ipie.service.iam.event;

/** Event types this service publishes on its own exchange ({@code ipie-iam-service.events}). */
public enum IamEventType {

    /**
     * A Keycloak account has been created for a user, in response to ipie-user-service's
     * {@code ACCOUNT_PROVISIONING_REQUESTED}. Carries the Keycloak id back so the requesting
     * service can record it and move the registration on.
     *
     * <p>Carries <b>no</b> credential-setup token: ipie-user-service has no business handling one,
     * and the fewer services a bearer secret passes through the better.
     */
    ACCOUNT_PROVISIONED,

    /**
     * Asks ipie-communication-service to email a registrant the link that lets them choose their
     * first password. Published straight from this service to comms, deliberately bypassing
     * ipie-user-service - a token that can set an account's password should reach exactly the two
     * services that need it, the one that issued it and the one that mails it.
     */
    ACCOUNT_CREDENTIAL_SETUP_REQUESTED;

    public static final int CONTRACT_VERSION = 1;
}
