package in.gov.ipie.service.iam.security;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.security.secret.DigestSecretHasher;
import in.gov.ipie.common.security.secret.SecretGenerator;
import in.gov.ipie.common.security.secret.SecretHasher;

/**
 * Issues and fingerprints the single-use token that authorises setting an initial password.
 *
 * <p>The primitives are the platform's; what this class contributes is the pairing. The token is 256
 * bits of {@code SecureRandom}, so the unkeyed {@link DigestSecretHasher} is the correct mode here -
 * there is no search space to enumerate and a pepper would add a key to manage for no gain. That is
 * the opposite call from ipie-user-service's registration secrets, and it is opposite for a reason
 * worth stating rather than leaving to be rediscovered: it is the entropy of the input that decides,
 * not the sensitivity of what the secret unlocks.
 *
 * <p>Only the fingerprint is ever stored, so a database read yields nothing that can be presented as
 * a token.
 */
@Component
public class CredentialSetupTokens {

    private final SecretGenerator generator = new SecretGenerator();
    private final SecretHasher hasher = new DigestSecretHasher();

    public String generate() {
        return generator.newToken();
    }

    public String fingerprint(String token) {
        return hasher.hash(token);
    }
}
