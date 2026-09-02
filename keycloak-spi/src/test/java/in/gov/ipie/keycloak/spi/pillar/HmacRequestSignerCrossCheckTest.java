package in.gov.ipie.keycloak.spi.pillar;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.security.hmac.HmacSignature;

/**
 * The regression guard {@link HmacRequestSigner}'s own Javadoc promises: since this module cannot
 * depend on {@code common-security} at runtime (would bundle Spring/Jackson into Keycloak's own
 * classloader), its HMAC-signing algorithm is a hand-written, JDK-only copy of {@code
 * HmacSignature}'s - a duck-typed copy the Java compiler cannot verify stays in sync on its own.
 * This test is what actually verifies it, by calling both implementations with identical inputs
 * and asserting byte-for-byte identical output. {@code common-security} is a test-only dependency
 * here (see this module's own {@code build.gradle}) - never bundled into the artifact deployed
 * into Keycloak's {@code providers/} directory.
 */
class HmacRequestSignerCrossCheckTest {

    @Test
    void sign_producesIdenticalOutputToCommonSecuritysHmacSignature() {
        String secret = "shared-secret";
        String method = "POST";
        String path = "/internal/pillar-links/resolve";
        String timestamp = "2026-08-02T10:15:30Z";
        String nonce = "11111111-1111-1111-1111-111111111111";
        byte[] body = "{\"pillarType\":\"IBBI\",\"externalPillarId\":\"ext-123\"}".getBytes(StandardCharsets.UTF_8);

        String expected = HmacSignature.sign(secret, method, path, timestamp, nonce, body);
        String actual = HmacRequestSigner.sign(secret, method, path, timestamp, nonce, body);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void sign_producesIdenticalOutputToCommonSecuritysHmacSignature_forAnEmptyBody() {
        String secret = "another-secret";
        String method = "POST";
        String path = "/internal/pillar-links/resolve";
        String timestamp = "2026-08-02T10:16:00Z";
        String nonce = "22222222-2222-2222-2222-222222222222";
        byte[] body = new byte[0];

        String expected = HmacSignature.sign(secret, method, path, timestamp, nonce, body);
        String actual = HmacRequestSigner.sign(secret, method, path, timestamp, nonce, body);

        assertThat(actual).isEqualTo(expected);
    }
}
