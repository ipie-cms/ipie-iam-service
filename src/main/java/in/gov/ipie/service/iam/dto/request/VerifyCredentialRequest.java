package in.gov.ipie.service.iam.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Keycloak's login-path question, asked by the ipie-keycloak-spi authenticators: is this the right
 * password for this account?
 *
 * <p><b>No {@code PasswordPolicy} validation here, deliberately.</b> This carries a password already
 * chosen at some point in the past, not one being set now. Applying the current policy would reject
 * a legitimate login the moment the policy is tightened, locking out every user whose password
 * predates the change - a policy change must force a reset, never a silent lockout.
 */
public record VerifyCredentialRequest(

        @NotNull
        UUID keycloakUserId,

        @NotBlank
        String password) {
}
