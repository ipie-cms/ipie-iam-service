package in.gov.ipie.keycloak.spi.pillar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import in.gov.ipie.keycloak.spi.config.SpiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Calls ipie-iam-service's {@code POST /internal/pillar-links/resolve} on behalf of the
 * custom first-broker-login Authenticator - moved from ipie-user-service per ADR-001 ("Placement
 * of pillar_links Data and /resolve Endpoint"): the data stays in ipie-user-service, but the
 * endpoint on this login-critical hot path now reads a read-optimised projection in
 * ipie-iam-service instead, so this service's own availability no longer depends on
 * ipie-user-service's. Plain JDK {@link HttpClient} (zero new dependency, matches this module's
 * compileOnly-everything build) - Spring's {@code OAuth2AuthorizedClientManager} machinery {@code
 * KeycloakUserManagementClient} uses isn't available inside Keycloak's own runtime, so the
 * client-credentials grant is performed manually here, mirroring that class's "authenticate as
 * your own narrowly-scoped client" pattern in plain Java. All endpoints/credentials come from
 * environment variables (this module has no Spring {@code @Value}/{@code application.yml} to draw
 * from either) - same {@code IPIE_*} naming convention every other service in this platform
 * already uses.
 *
 * <p>Every call also carries an HMAC signature ({@code X-Signature}/{@code X-Timestamp}/{@code
 * X-Nonce}/{@code X-Signing-Key-Id}) - additive defense in depth on top of the bearer token above,
 * verified by ipie-iam-service's {@code HmacSignatureVerificationFilter} against the matching
 * {@code ipie.security.hmac.keys.spi-to-iam} entry there. See {@link HmacRequestSigner}'s Javadoc
 * for why the signing algorithm is hand-written here rather than shared.
 */
class PillarLinkResolverClient {

    private static final String RESOLVE_PATH = "/internal/pillar-links/resolve";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(SpiConfig.duration(SpiConfig.PILLAR_CONNECT_TIMEOUT_MS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Endpoints and secrets come from SpiConfig: localhost defaults in dev only, required
    // everywhere else, checked at startup rather than at the first login.
    private final String keycloakBaseUrl = SpiConfig.get(SpiConfig.KEYCLOAK_BASE_URL);
    private final String keycloakRealm = SpiConfig.get(SpiConfig.KEYCLOAK_REALM);
    private final String spiClientId = SpiConfig.get(SpiConfig.SPI_CLIENT_ID);
    private final String spiClientSecret = SpiConfig.get(SpiConfig.SPI_CLIENT_SECRET);
    private final String iamServiceBaseUrl = SpiConfig.get(SpiConfig.IAM_SERVICE_BASE_URL);
    private final String hmacKeyId = SpiConfig.get(SpiConfig.HMAC_KEY_ID_IAM);
    private final String hmacSecret = SpiConfig.get(SpiConfig.HMAC_SECRET_IAM);

    PillarResolveResult resolve(String pillarType, String externalPillarId, String ipieId) {
        String token = fetchClientCredentialsToken();

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("pillarType", pillarType);
        body.put("externalPillarId", externalPillarId);
        body.put("ipieId", ipieId);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(iamServiceBaseUrl + RESOLVE_PATH))
                    .timeout(SpiConfig.duration(SpiConfig.PILLAR_REQUEST_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token);
            addSignatureHeaders(requestBuilder, "POST", RESOLVE_PATH, requestBody.getBytes(StandardCharsets.UTF_8));
            HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new PillarResolveException(
                        "ipie-iam-service's resolve endpoint returned HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), PillarResolveResult.class);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PillarResolveException("Failed to call ipie-iam-service's resolve endpoint", e);
        }
    }

    private String fetchClientCredentialsToken() {
        String form = "grant_type=client_credentials&client_id=" + spiClientId + "&client_secret=" + spiClientSecret;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(keycloakBaseUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token"))
                    .timeout(SpiConfig.duration(SpiConfig.PILLAR_REQUEST_TIMEOUT_MS))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new PillarResolveException(
                        "Failed to obtain a client-credentials token for " + spiClientId + " (HTTP " + response.statusCode() + ")");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
            Object accessToken = parsed.get("access_token");
            if (accessToken == null) {
                throw new PillarResolveException("No access_token in client-credentials response");
            }
            return accessToken.toString();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PillarResolveException("Failed to obtain a client-credentials token for " + spiClientId, e);
        }
    }

    /**
     * Adds the four HMAC signing headers {@code HmacSignatureVerificationFilter} verifies -
     * mirrors {@code common-client}'s {@code HmacSigningInterceptor} exactly, in plain Java (see
     * {@link HmacRequestSigner}'s Javadoc for why it cannot just depend on that class).
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
