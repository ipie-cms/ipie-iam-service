package in.gov.ipie.keycloak.spi.login;

import in.gov.ipie.keycloak.spi.config.SpiConfig;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Registers {@link LoginNotificationEventListenerProvider} under provider id {@code
 * ipie-login-notifier} - referenced by {@code deploy/keycloak/realm-export.json}'s realm-level
 * {@code eventsListeners}. Discovered via the standard Java {@code ServiceLoader} registration in
 * {@code META-INF/services/org.keycloak.events.EventListenerProviderFactory}, mirroring how
 * {@code PillarLinkResolverAuthenticatorFactory} is discovered for its own SPI type.
 */
public class LoginNotificationEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "ipie-login-notifier";

    private final LoginNotificationClient client = new LoginNotificationClient();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LoginNotificationEventListenerProvider(client);
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
