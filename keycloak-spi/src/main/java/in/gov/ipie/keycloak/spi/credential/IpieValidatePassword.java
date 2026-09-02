package in.gov.ipie.keycloak.spi.credential;

import java.util.UUID;

import in.gov.ipie.keycloak.spi.config.SpiConfig;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.directgrant.ValidatePassword;
import org.keycloak.events.Errors;
import org.keycloak.models.UserModel;

/**
 * Replaces Keycloak's built-in direct-grant password check with one that asks ipie-iam-service.
 *
 * <p>This is the authenticator the {@code password} grant runs through - the grant ipie-web's login
 * page uses today ({@code keycloakApi.ts}). Keycloak holds no password to compare against
 * (ARCHITECTURE_WORKING_PLAN.md, D1), so the built-in step would fail every login on an account provisioned
 * under the current design.
 *
 * <p>Extends {@code ValidatePassword} rather than reimplementing the step, so everything around the
 * comparison - how the grant retrieves the form field, the error response shape clients already
 * parse - stays Keycloak's. Only the answer to "is this password correct" changes hands.
 *
 * <p>{@code ValidatePassword} extends {@code AbstractDirectGrantAuthenticator}, which implements both
 * {@code Authenticator} and {@code AuthenticatorFactory}, so this one class is its own factory and is
 * what gets registered in {@code META-INF/services}.
 *
 * <p><b>Fails closed.</b> If iam cannot be reached the login is refused, never allowed through. The
 * client is told "temporarily unavailable" rather than "invalid credentials", because telling a user
 * their password is wrong when the system simply could not check it sends them to reset a password
 * that was fine.
 */
public class IpieValidatePassword extends ValidatePassword {

    public static final String PROVIDER_ID = "ipie-direct-grant-validate-password";

    private static final Logger LOG = Logger.getLogger(IpieValidatePassword.class);

    private final CredentialVerifyClient verifyClient = new CredentialVerifyClient();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            // The username step ahead of this one did not resolve a user. Nothing to verify against.
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.failure(
                    AuthenticationFlowError.INVALID_USER,
                    errorResponse(401, "invalid_grant", "Invalid user credentials"));
            return;
        }

        String password = retrievePassword(context);
        if (password == null || password.isEmpty()) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    errorResponse(401, "invalid_grant", "Invalid user credentials"));
            return;
        }

        UUID keycloakUserId;
        try {
            keycloakUserId = UUID.fromString(user.getId());
        } catch (IllegalArgumentException e) {
            // A federated user id ("f:<provider>:<external>") rather than a local UUID. Those users
            // authenticate at their own identity provider and never reach a password step here, so
            // arriving with one is a misconfigured flow - refuse rather than guess.
            LOG.warnf("Direct-grant password step reached for non-local user id '%s' - refusing", user.getId());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(
                    AuthenticationFlowError.INVALID_USER,
                    errorResponse(401, "invalid_grant", "Invalid user credentials"));
            return;
        }

        boolean valid;
        try {
            valid = verifyClient.verify(keycloakUserId, password);
        } catch (CredentialVerificationException e) {
            LOG.error("Could not verify credentials against ipie-iam-service - failing the login closed", e);
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    errorResponse(503, "temporarily_unavailable", "Unable to verify credentials, please retry"));
            return;
        }

        if (!valid) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    errorResponse(401, "invalid_grant", "Invalid user credentials"));
            return;
        }

        // Keycloak still owns everything after the credential check - required actions, the account
        // enabled/disabled state, session creation and token issuance. This step's only job is to
        // answer the password question, so it hands back to the flow here.
        context.success();
    }

    @Override
    public void init(Config.Scope config) {
        // This class is its own factory (see the class Javadoc), so this is the startup hook.
        // Configuration comes from the environment rather than Config.Scope - there is no Spring
        // context inside Keycloak - and validating it here is what makes a misconfigured deployment
        // fail at startup naming the missing variable, instead of failing every login closed with
        // 503 temporarily_unavailable. See SpiConfig.
        SpiConfig.validateAll();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "iPIE Direct Grant - Validate Password";
    }

    @Override
    public String getHelpText() {
        return "Validates the password of the direct-grant request against ipie-iam-service, which is the "
                + "credential authority for this platform. Keycloak stores no password of its own.";
    }
}
