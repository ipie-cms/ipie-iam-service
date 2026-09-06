package in.gov.ipie.keycloak.spi.credential;

import java.util.UUID;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;

/**
 * The browser-flow counterpart of {@link IpieValidatePassword}: the login form's password check,
 * answered by ipie-iam-service instead of by Keycloak's own credential store.
 *
 * <p>Overrides <b>only</b> {@code validatePassword}, deliberately. Its caller,
 * {@code validateUserAndPassword}, is what performs the brute-force check, the disabled-account
 * check and the form re-rendering around it. Overriding {@code authenticate} instead would have
 * silently dropped all of that - the login would still work, and the protections would quietly not.
 *
 * <p><b>Fails closed</b> for the same reason as the direct-grant variant: an unreachable iam means
 * the password could not be checked, which is not the same as the password being wrong, and must
 * never let a login through.
 */
public class IpieUsernamePasswordForm extends UsernamePasswordForm {

    private static final Logger LOG = Logger.getLogger(IpieUsernamePasswordForm.class);

    private final CredentialVerifyClient verifyClient = new CredentialVerifyClient();

    @Override
    public boolean validatePassword(
            AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData, boolean clearUser) {

        String password = inputData.getFirst("password");
        if (password == null || password.isEmpty()) {
            return badPassword(context, clearUser);
        }

        UUID keycloakUserId;
        try {
            keycloakUserId = UUID.fromString(user.getId());
        } catch (IllegalArgumentException e) {
            // Federated user ids ("f:<provider>:<external>") authenticate at their own IdP and never
            // have a password here - see IpieValidatePassword for the same guard.
            LOG.warnf("Password form reached for non-local user id '%s' - refusing", user.getId());
            return badPassword(context, clearUser);
        }

        try {
            if (verifyClient.verify(keycloakUserId, password)) {
                return true;
            }
        } catch (CredentialVerificationException e) {
            LOG.error("Could not verify credentials against ipie-iam-service - failing the login closed", e);
            context.form().setError(Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
            return false;
        }
        return badPassword(context, clearUser);
    }

    /**
     * The same outcome Keycloak's own form produces for a wrong password: the generic
     * "invalid username or password" message, which deliberately does not reveal whether the account
     * exists.
     */
    private boolean badPassword(AuthenticationFlowContext context, boolean clearUser) {
        if (clearUser) {
            context.clearUser();
        }
        context.form().addError(new FormMessage(Messages.INVALID_USER));
        return false;
    }
}
