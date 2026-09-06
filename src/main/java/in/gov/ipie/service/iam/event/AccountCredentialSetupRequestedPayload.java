package in.gov.ipie.service.iam.event;

import java.util.UUID;

/**
 * Everything ipie-communication-service needs to email a registrant their set-password link.
 *
 * <p><b>Carries a bearer secret.</b> {@code setupToken} lets whoever holds it choose this account's
 * password, so it is the one thing on this platform's outbox that must be treated like a credential
 * in transit even though it is not one. What makes that acceptable, where a password would not be:
 * it is single-use, it expires, it is stored only as a SHA-256 fingerprint, and it authorises
 * exactly one irreversible-once operation. A password is reusable indefinitely and identifies a
 * human across systems - which is why it never travels this way.
 *
 * <p>The same reasoning already applies to {@code RegistrationEmailOtpRequestedPayload}'s OTP code,
 * so this is an established pattern on this platform rather than a new exception.
 *
 * <p>Consumers must keep it out of logs and the notification log body - see
 * {@code NotificationServiceImpl}'s masking of the emailed copy.
 */
public record AccountCredentialSetupRequestedPayload(
        UUID userId,
        UUID keycloakUserId,
        String email,
        String fullName,
        String setupToken) {
}
