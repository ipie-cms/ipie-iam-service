package in.gov.ipie.keycloak.spi.login;

import java.time.Instant;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

/**
 * Notifies ipie-user-service of every successful login (see {@link LoginNotificationClient}) so
 * it can publish {@code USER_LOGGED_IN} for ipie-communication-service to act on. Only {@link
 * EventType#LOGIN} (a successful login) is acted on - {@code LOGIN_ERROR} and every other event
 * type are ignored; alerting on failed-login attempts would be a separate, security-motivated
 * feature, not what this class does. Registered via {@code
 * deploy/keycloak/realm-export.json}'s realm-level {@code eventsListeners}, discovered through
 * the standard Java {@code ServiceLoader} registration in {@code
 * META-INF/services/org.keycloak.events.EventListenerProviderFactory}.
 *
 * <p>Runs inline with the login request thread (Keycloak's event-listener contract), so {@link
 * #onEvent(Event)} must never block for long or throw - {@link LoginNotificationClient} already
 * enforces both (short timeouts, every exception caught and logged), so this class does not need
 * its own additional try/catch to stay safe.
 */
public class LoginNotificationEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(LoginNotificationEventListenerProvider.class);

    private final LoginNotificationClient client;

    LoginNotificationEventListenerProvider(LoginNotificationClient client) {
        this.client = client;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN) {
            return;
        }
        LOG.debugf("Notifying ipie-user-service of a login for user %s", event.getUserId());
        client.notifyLogin(event.getUserId(), Instant.ofEpochMilli(event.getTime()), event.getIpAddress());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Admin actions (realm/console management) are out of scope for a user-facing login alert.
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
