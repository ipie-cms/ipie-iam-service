package in.gov.ipie.keycloak.spi.pillar;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A byte-for-byte, JDK-only copy of {@code ipie-common-libs}'s {@code
 * in.gov.ipie.common.security.hmac.HmacSignature} - this module cannot depend on
 * {@code common-security} at all (it runs inside Keycloak's own Quarkus runtime with every
 * Spring/Jackson dependency {@code compileOnly}; a real dependency would bundle Spring/Jackson
 * into Keycloak's own classloader, defeating the classloader isolation
 * {@code PillarLinkResolverClient}'s own Javadoc already documents), so the canonical-string
 * + HMAC-SHA256 algorithm is hand-written here instead of shared.
 *
 * <p><b>Must be kept byte-for-byte in sync with {@code HmacSignature.sign} - canonical string
 * {@code METHOD\nPATH\nTIMESTAMP\nNONCE\nSHA256HEX(body)}, then {@code HmacSHA256}, hex-encoded.
 * </b> {@code HmacRequestSignerCrossCheckTest} in this module's own test sources asserts this
 * produces identical output to {@code HmacSignature} for fixed inputs - the regression guard
 * against these two ever drifting apart, since the Java compiler cannot catch a duck-typed copy
 * going out of sync the way it would a shared dependency.
 *
 * <p>Public (not package-private) so {@code in.gov.ipie.keycloak.spi.login.LoginNotificationClient}
 * can reuse it too - a second hand-written copy of this exact algorithm within the same module
 * (as opposed to across the common-security module boundary this class's Javadoc already
 * explains) would only add a second place for the two to drift apart from each other.
 */
public final class HmacRequestSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final HexFormat HEX = HexFormat.of();

    private HmacRequestSigner() {
    }

    /** Computes the hex-encoded HMAC-SHA256 signature over the canonical string built from these fields. */
    public static String sign(String secret, String method, String path, String timestamp, String nonce, byte[] body) {
        String canonical = canonicalString(method, path, timestamp, nonce, body);
        return hmacHex(secret, canonical);
    }

    private static String canonicalString(String method, String path, String timestamp, String nonce, byte[] body) {
        String bodyHash = sha256Hex(body == null ? new byte[0] : body);
        return method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyHash;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HEX.formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " is a JDK-mandatory algorithm and must always be available", e);
        }
    }

    private static String hmacHex(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HEX.formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HMAC_ALGORITHM + " is a JDK-mandatory algorithm and must always be available", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid HMAC signing key", e);
        }
    }
}
