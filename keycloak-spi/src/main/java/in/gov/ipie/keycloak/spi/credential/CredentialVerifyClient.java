package in.gov.ipie.keycloak.spi.credential;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.ipie.keycloak.spi.config.SpiConfig;
import in.gov.ipie.keycloak.spi.pillar.HmacRequestSigner;

/**
 * Asks ipie-iam-service whether a submitted password is correct.
 *
 * <p>Keycloak no longer stores passwords (ARCHITECTURE_WORKING_PLAN.md, D1) - the Argon2id hash lives in
 * ipie-iam-service, which is the credential authority. So the authenticators in this package cannot
 * answer the question themselves and must ask.
 *
 * <p>Mirrors {@code PillarLinkResolverClient} exactly in shape: a client-credentials token from
 * Keycloak, HMAC signing headers, and env-var configuration (this module runs inside Keycloak's own
 * runtime and has no Spring context or {@code application.yml} to draw from).
 *
 * <p><b>Timeouts are deliberately tighter than the resolve client's.</b> This sits on the interactive
 * login path with a person waiting on it, so a slow dependency should surface as a failed login
 * quickly rather than a hung browser. There is no retry: a password check is not safely repeatable
 * from here - each attempt is a login attempt as far as brute-force counting is concerned.
 */
class CredentialVerifyClient {

    private static final String VERIFY_PATH = "/internal/credentials/verify";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(SpiConfig.duration(SpiConfig.CREDENTIAL_CONNECT_TIMEOUT_MS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Endpoints and secrets come from SpiConfig, which applies localhost defaults in dev only and
    // refuses to let Keycloak start elsewhere without them - see the note there on the 9090 default
    // that made every login fail closed with 503 temporarily_unavailable.
    private final String keycloakBaseUrl = SpiConfig.get(SpiConfig.KEYCLOAK_BASE_URL);
    private final String keycloakRealm = SpiConfig.get(SpiConfig.KEYCLOAK_REALM);
    private final String spiClientId = SpiConfig.get(SpiConfig.SPI_CLIENT_ID);
    private final String spiClientSecret = SpiConfig.get(SpiConfig.SPI_CLIENT_SECRET);
    private final String iamServiceBaseUrl = SpiConfig.get(SpiConfig.IAM_SERVICE_BASE_URL);
    private final String hmacKeyId = SpiConfig.get(SpiConfig.HMAC_KEY_ID_IAM);
    private final String hmacSecret = SpiConfig.get(SpiConfig.HMAC_SECRET_IAM);

    /**
     * @return {@code true} if the password matches the stored hash, {@code false} if it does not
     * @throws CredentialVerificationException if no answer could be obtained - the caller must fail
     *     the login rather than guess
     */
    boolean verify(UUID keycloakUserId, String password) {
        String token = fetchClientCredentialsToken();
        try {
            // The password is in the body of a TLS-protected call to the credential authority, which
            // is the only route the platform's credential rule permits. It is never logged here and
            // never put on an event.
            String requestBody = objectMapper.writeValueAsString(
                    Map.of("keycloakUserId", keycloakUserId.toString(), "password", password));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(iamServiceBaseUrl + VERIFY_PATH))
                    .timeout(SpiConfig.duration(SpiConfig.CREDENTIAL_REQUEST_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token);
            addSignatureHeaders(requestBuilder, "POST", VERIFY_PATH, requestBody.getBytes(StandardCharsets.UTF_8));

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build(),
                    HttpResponse.BodyHandlers.ofString());

            // Anything but 200 means the check did not happen. Note what is NOT done here: a 401 or
            // 403 is not read as "wrong password" - it means this SPI's own credentials or signature
            // were rejected, a deployment fault that must not be reported to a user as a bad login.
            if (response.statusCode() != 200) {
                throw new CredentialVerificationException(
                        "ipie-iam-service's credential verify endpoint returned HTTP " + response.statusCode());
            }

            Boolean valid = objectMapper.readTree(response.body()).path("valid").asBoolean(false);
            return Boolean.TRUE.equals(valid);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CredentialVerificationException("Failed to call ipie-iam-service's credential verify endpoint", e);
        }
    }

    private String fetchClientCredentialsToken() {
        String form = "grant_type=client_credentials&client_id=" + spiClientId + "&client_secret=" + spiClientSecret;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(keycloakBaseUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token"))
                    .timeout(SpiConfig.duration(SpiConfig.CREDENTIAL_REQUEST_TIMEOUT_MS))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CredentialVerificationException(
                        "Failed to obtain a client-credentials token for " + spiClientId
                                + " (HTTP " + response.statusCode() + ")");
            }
            String accessToken = objectMapper.readTree(response.body()).path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new CredentialVerificationException("No access_token in the client-credentials response");
            }
            return accessToken;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CredentialVerificationException(
                    "Failed to obtain a client-credentials token for " + spiClientId, e);
        }
    }

    /** The four headers {@code HmacSignatureVerificationFilter} verifies - see {@link HmacRequestSigner}. */
    private void addSignatureHeaders(HttpRequest.Builder requestBuilder, String method, String path, byte[] body) {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        requestBuilder
                .header("X-Signature", HmacRequestSigner.sign(hmacSecret, method, path, timestamp, nonce, body))
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signing-Key-Id", hmacKeyId);
    }

}
