package in.gov.ipie.keycloak.spi.credential;

import java.util.List;

import in.gov.ipie.keycloak.spi.config.SpiConfig;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Registers {@link IpieUsernamePasswordForm} so it can replace {@code auth-username-password-form}
 * in the realm's browser flow.
 *
 * <p>A separate factory class is needed here only because {@code UsernamePasswordForm} is an
 * {@code Authenticator} alone - unlike the direct-grant hierarchy, where
 * {@code AbstractDirectGrantAuthenticator} is its own factory and {@link IpieValidatePassword}
 * therefore needs none.
 */
public class IpieUsernamePasswordFormFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ipie-auth-username-password-form";

    // Stateless: the authenticator holds only an HTTP client, so one instance serves every login.
    // Mirrors PillarLinkResolverAuthenticatorFactory's SINGLETON.
    private static final IpieUsernamePasswordForm SINGLETON = new IpieUsernamePasswordForm();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "iPIE Username Password Form";
    }

    @Override
    public String getReferenceCategory() {
        return "password";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        // REQUIRED only: a password step that a realm could mark ALTERNATIVE or DISABLED is a
        // password step that can be configured away, which for the only credential check in the
        // browser flow would mean an unauthenticated login.
        return new AuthenticationExecutionModel.Requirement[] {AuthenticationExecutionModel.Requirement.REQUIRED};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Username/password login form that validates the password against ipie-iam-service, the credential "
                + "authority for this platform, rather than against Keycloak's own credential store.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // Configuration comes from the environment, not from Config.Scope: this module runs inside
        // Keycloak and has no Spring context. Validating it HERE is what makes a misconfigured
        // deployment fail at startup, naming the missing variable, instead of starting cleanly and
        // failing every login closed with 503 temporarily_unavailable. See SpiConfig.
        SpiConfig.validateAll();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No cross-provider wiring needed.
    }

    @Override
    public void close() {
        // No resources held.
    }
}
