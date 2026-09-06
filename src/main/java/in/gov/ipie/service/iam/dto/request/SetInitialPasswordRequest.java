package in.gov.ipie.service.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import in.gov.ipie.common.security.password.PasswordPolicy;

/**
 * Sets the first password on an account provisioned without credentials, authorised by the one-time
 * token from the registrant's email rather than by a JWT - they have no credentials yet, so there is
 * nothing to authenticate them with. Possession of the mailbox is the proof.
 *
 * <p>{@link PasswordPolicy} is enforced here because this service is now the control, not a mirror
 * of Keycloak's realm policy: Keycloak no longer stores or validates passwords at all
 * (ARCHITECTURE_WORKING_PLAN.md, D1), so nothing behind this would catch a weak one.
 */
public record SetInitialPasswordRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String password) {
}
