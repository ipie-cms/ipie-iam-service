package in.gov.ipie.keycloak.spi.pillar;

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
 * Registers {@link PillarLinkResolverAuthenticator} under provider id {@code
 * ipie-pillar-link-resolver} - referenced by {@code
 * deploy/keycloak/realm-export.json}'s {@code authenticationFlows[]} entry {@code
 * ipie-pillar-first-broker-login}. Discovered via the standard Java {@code ServiceLoader}
 * registration in {@code META-INF/services/org.keycloak.authentication.AuthenticatorFactory}.
 */
public class PillarLinkResolverAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ipie-pillar-link-resolver";

    private static final PillarLinkResolverAuthenticator SINGLETON = new PillarLinkResolverAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "iPIE Pillar Link Resolver";
    }

    @Override
    public String getReferenceCategory() {
        return "pillar-sso";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {AuthenticationExecutionModel.Requirement.REQUIRED};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Resolves a brokered pillar login (IBBI/NCLT/NCLAT/MCA) to an existing ipie user via "
                + "ipie-user-service's authoritative pillar-link table, instead of Keycloak's default "
                + "first-broker-login behavior.";
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
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
