package in.gov.ipie.service.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import in.gov.ipie.common.security.password.PasswordPolicy;

/**
 * Changes the password of the authenticated caller.
 *
 * <p>{@code currentPassword} carries no policy constraint for the same reason
 * {@code VerifyCredentialRequest} carries none - it is an existing password being proved, not a new
 * one being chosen, and rejecting it against today's rules would make a tightened policy lock people
 * out of changing the very password that no longer complies.
 */
public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword) {
}
