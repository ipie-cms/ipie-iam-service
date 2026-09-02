package in.gov.ipie.service.iam.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.service.iam.domain.CredentialSetupToken;
import in.gov.ipie.service.iam.domain.UserCredential;
import in.gov.ipie.service.iam.exception.InvalidCredentialSetupTokenException;
import in.gov.ipie.service.iam.exception.InvalidCurrentPasswordException;
import in.gov.ipie.service.iam.repository.CredentialSetupTokenRepository;
import in.gov.ipie.service.iam.repository.UserCredentialRepository;
import in.gov.ipie.service.iam.security.CredentialSetupTokens;
import in.gov.ipie.service.iam.security.PasswordHasher;

/**
 * {@link CredentialService} implementation.
 *
 * <p><b>Nothing here logs, audits, or returns a password.</b> The {@code @Auditable} annotations
 * below record only the account id and the action - deliberately not {@code #result} or any
 * argument, because a credential must never reach an audit event in the first place.
 * {@code AuditValueMasker} would catch the field name, but relying on it would be treating a
 * backstop as the control.
 */
@Service
public class CredentialServiceImpl implements CredentialService {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialServiceImpl.class);

    private final UserCredentialRepository credentialRepository;
    private final CredentialSetupTokenRepository setupTokenRepository;
    private final PasswordHasher passwordHasher;
    private final CredentialSetupTokens setupTokens;
    private final Duration setupTokenTtl;

    public CredentialServiceImpl(
            UserCredentialRepository credentialRepository,
            CredentialSetupTokenRepository setupTokenRepository,
            PasswordHasher passwordHasher,
            CredentialSetupTokens setupTokens,
            @Value("${ipie.credentials.setup-token-ttl:PT48H}") Duration setupTokenTtl) {
        this.credentialRepository = credentialRepository;
        this.setupTokenRepository = setupTokenRepository;
        this.passwordHasher = passwordHasher;
        this.setupTokens = setupTokens;
        this.setupTokenTtl = setupTokenTtl;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verify(UUID keycloakUserId, String rawPassword) {
        Optional<UserCredential> credential = credentialRepository.findByKeycloakUserId(keycloakUserId);
        if (credential.isEmpty()) {
            // Provisioned but never set up. Not an error condition - the account genuinely has no
            // password, and the honest answer to "is this the right one" is no.
            LOG.debug("No credential stored for Keycloak user {}", keycloakUserId);
            return false;
        }
        return passwordHasher.matches(rawPassword, credential.get().passwordHash());
    }

    @Override
    @Transactional
    @Auditable(
            action = "CREDENTIAL_SETUP_TOKEN_ISSUED", entityType = "KEYCLOAK_ACCOUNT",
            entityId = "#keycloakUserId", eventType = AuditEventType.SECURITY)
    public String issueSetupToken(UUID keycloakUserId) {
        Instant now = Instant.now();
        // Any earlier link dies here. Two live links to one account would mean an older email,
        // possibly already forwarded or leaked, still able to take the account over.
        setupTokenRepository.consumeOutstandingFor(keycloakUserId, now);

        String token = setupTokens.generate();
        setupTokenRepository.save(new CredentialSetupToken(
                setupTokens.fingerprint(token), keycloakUserId, now, now.plus(setupTokenTtl), null));
        return token;
    }

    @Override
    @Transactional
    @Auditable(
            action = "CREDENTIAL_INITIAL_PASSWORD_SET", entityType = "KEYCLOAK_ACCOUNT",
            entityId = "#result", eventType = AuditEventType.SECURITY)
    public UUID setInitialPassword(String token, String rawPassword) {
        Instant now = Instant.now();
        CredentialSetupToken setupToken = setupTokenRepository.findByTokenHash(setupTokens.fingerprint(token))
                .filter(candidate -> candidate.isSpendableAt(now))
                .orElseThrow(InvalidCredentialSetupTokenException::new);

        credentialRepository.save(new UserCredential(
                setupToken.keycloakUserId(), passwordHasher.hash(rawPassword), PasswordHasher.ALGORITHM, now));

        // Spent only after the credential is stored. Both writes are in this one transaction, so a
        // failure anywhere leaves the token unspent and the link still usable - the user retries
        // rather than being stranded with a dead link and no password.
        setupTokenRepository.save(setupToken.consumedAt(now));
        LOG.info("Initial password set for Keycloak user {}", setupToken.keycloakUserId());
        return setupToken.keycloakUserId();
    }

    @Override
    @Transactional
    @Auditable(
            action = "CREDENTIAL_PASSWORD_CHANGED", entityType = "KEYCLOAK_ACCOUNT",
            entityId = "#keycloakUserId", eventType = AuditEventType.SECURITY)
    public void changePassword(UUID keycloakUserId, String currentPassword, String newPassword) {
        UserCredential credential = credentialRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(InvalidCurrentPasswordException::new);
        if (!passwordHasher.matches(currentPassword, credential.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        credentialRepository.save(new UserCredential(
                keycloakUserId, passwordHasher.hash(newPassword), PasswordHasher.ALGORITHM, Instant.now()));
        LOG.info("Password changed for Keycloak user {}", keycloakUserId);
    }
}
