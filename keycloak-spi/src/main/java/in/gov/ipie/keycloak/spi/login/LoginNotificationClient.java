package in.gov.ipie.keycloak.spi.login;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import in.gov.ipie.keycloak.spi.config.SpiConfig;
import in.gov.ipie.keycloak.spi.pillar.HmacRequestSigner;

/**
 * Calls ipie-user-service's {@code POST /internal/logins/notify} on every successful login (see
 * {@link LoginNotificationEventListenerProvider}). Closely mirrors {@code
 * in.gov.ipie.keycloak.spi.pillar.PillarLinkResolverClient} (same plain-JDK-{@link
 * HttpClient}, env-var-configured, hand-written-HMAC pattern - see that class's Javadoc for why),
 * with one deliberate difference: a login notification is never decision-critical the way
 * pillar-link resolution is (that call decides whether the login itself succeeds) - this one
 * must never slow down or fail a login, so every method here swallows its own exceptions and logs
 * a warning instead of throwing. Fire-and-forget by design.
 */
class LoginNotificationClient {

    private static final Logger LOG = Logger.getLogger(LoginNotificationClient.class);
    private static final String NOTIFY_PATH = "/internal/logins/notify";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(SpiConfig.duration(SpiConfig.LOGIN_NOTIFY_CONNECT_TIMEOUT_MS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Endpoints and secrets come from SpiConfig: localhost defaults in dev only, required
    // everywhere else, checked at startup rather than at the first login.
    private final String keycloakBaseUrl = SpiConfig.get(SpiConfig.KEYCLOAK_BASE_URL);
    private final String keycloakRealm = SpiConfig.get(SpiConfig.KEYCLOAK_REALM);
    private final String spiClientId = SpiConfig.get(SpiConfig.SPI_CLIENT_ID);
    private final String spiClientSecret = SpiConfig.get(SpiConfig.SPI_CLIENT_SECRET);
    private final String userServiceBaseUrl = SpiConfig.get(SpiConfig.USER_SERVICE_BASE_URL);
    private final String hmacKeyId = SpiConfig.get(SpiConfig.HMAC_KEY_ID_USER);
    private final String hmacSecret = SpiConfig.get(SpiConfig.HMAC_SECRET_USER);

    /** Never throws - every failure is logged and swallowed, since a login must not depend on this call succeeding. */
    void notifyLogin(String keycloakUserId, Instant occurredAt, String sourceIp) {
        try {
            String token = fetchClientCredentialsToken();

            Map<String, Object> body = new HashMap<>();
            body.put("keycloakUserId", keycloakUserId);
            body.put("occurredAt", occurredAt.toString());
            body.put("sourceIp", sourceIp);
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(userServiceBaseUrl + NOTIFY_PATH))
                    .timeout(SpiConfig.duration(SpiConfig.LOGIN_NOTIFY_REQUEST_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token);
            addSignatureHeaders(requestBuilder, "POST", NOTIFY_PATH, requestBody.getBytes(StandardCharsets.UTF_8));
            HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                LOG.warnf(
                        "ipie-user-service's login-notify endpoint returned HTTP %d for user %s", response.statusCode(),
                        keycloakUserId);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("Failed to notify ipie-user-service of a login for user " + keycloakUserId, e);
        } catch (RuntimeException e) {
            LOG.warn("Failed to notify ipie-user-service of a login for user " + keycloakUserId, e);
        }
    }

    private String fetchClientCredentialsToken() throws IOException, InterruptedException {
        String form = "grant_type=client_credentials&client_id=" + spiClientId + "&client_secret=" + spiClientSecret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token"))
                .timeout(SpiConfig.duration(SpiConfig.LOGIN_NOTIFY_REQUEST_TIMEOUT_MS))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(
                    "Failed to obtain a client-credentials token for " + spiClientId + " (HTTP " + response.statusCode() + ")");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
        Object accessToken = parsed.get("access_token");
        if (accessToken == null) {
            throw new IOException("No access_token in client-credentials response");
        }
        return accessToken.toString();
    }

    /**
     * Adds the four HMAC signing headers {@code HmacSignatureVerificationFilter} verifies -
     * mirrors {@code common-client}'s {@code HmacSigningInterceptor} exactly, in plain Java (see
     * {@code HmacRequestSigner}'s Javadoc for why it cannot just depend on that class).
     */
    private void addSignatureHeaders(HttpRequest.Builder requestBuilder, String method, String path, byte[] body) {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = HmacRequestSigner.sign(hmacSecret, method, path, timestamp, nonce, body);
        requestBuilder
                .header("X-Signature", signature)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signing-Key-Id", hmacKeyId);
    }

}
