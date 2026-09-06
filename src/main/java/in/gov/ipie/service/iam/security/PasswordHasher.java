package in.gov.ipie.service.iam.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes and verifies passwords for the whole platform. This service is the credential authority
 * (ARCHITECTURE_WORKING_PLAN.md, D2), so this is the only class anywhere that sees a plaintext password.
 *
 * <p><b>Argon2id</b>, not BCrypt: it is memory-hard, which is what makes GPU and ASIC cracking
 * expensive rather than merely slow. The IT Act's SPDI Rules name passwords as sensitive personal
 * data, so the choice of function here is a compliance point and not only an engineering one.
 *
 * <p>The cost parameters below are OWASP's current recommendation for Argon2id. They are a
 * defensible starting point, <b>not a benchmarked decision</b> - the right way to set them is to
 * measure on the target hardware and pick the largest values that keep a single hash comfortably
 * under the login latency budget. Raising them later is a solved problem: {@link #needsRehash}
 * reports a hash made under weaker settings, so a login can transparently re-hash the password it
 * has just verified.
 */
@Component
public class PasswordHasher {

    /** 16 bytes - the standard salt width; more buys nothing once collisions are already implausible. */
    private static final int SALT_LENGTH = 16;

    /** 32 bytes of output, matching the 256-bit security level the rest of the platform assumes. */
    private static final int HASH_LENGTH = 32;

    /** One lane. Raise only alongside a measured decision about the login path's CPU budget. */
    private static final int PARALLELISM = 1;

    /** 19 MiB per hash (OWASP's Argon2id recommendation). This is the number that costs an attacker. */
    private static final int MEMORY_KIB = 19456;

    /** Two passes over that memory - OWASP's companion value for the memory setting above. */
    private static final int ITERATIONS = 2;

    /**
     * Recorded alongside every hash in {@code user_credentials.algorithm}. Encodes the cost
     * parameters, so a future migration can find rows made under weaker settings by querying rather
     * than by parsing each stored hash.
     */
    public static final String ALGORITHM = "argon2id-m" + MEMORY_KIB + "-t" + ITERATIONS + "-p" + PARALLELISM;

    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);

    /** Returns the encoded hash - {@code $argon2id$v=19$m=...,t=...,p=...$salt$hash} - salt and parameters included. */
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Constant-time comparison of a submitted password against a stored hash, delegated to the
     * encoder. Returns {@code false} for a malformed or unrecognised hash rather than throwing -
     * the caller's answer to "is this password correct" is no in either case, and distinguishing
     * them to a caller on the login path would leak the difference.
     */
    public boolean matches(String rawPassword, String encodedHash) {
        return encoder.matches(rawPassword, encodedHash);
    }

    /** True when {@code encodedHash} was produced under weaker parameters than {@link #ALGORITHM} describes. */
    public boolean needsRehash(String encodedHash) {
        return encoder.upgradeEncoding(encodedHash);
    }
}
