package in.gov.ipie.service.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sizes mirror {@code PermissionJpaEntity}'s columns exactly, so an over-long value is a 400 with a
 * field name rather than a constraint violation from the driver.
 *
 * <p>Both {@code name} and {@code resource} are constrained to SCREAMING_SNAKE_CASE. That is not
 * decoration: a permission name is matched literally against the string in a
 * {@code @RequiresPermission} and against a Keycloak realm-role name, so a value with a space or a
 * lowercase letter would be a permission nothing can ever match - and nothing downstream would
 * report it as wrong. The pattern is what every seeded permission already follows.
 */
public record CreatePermissionRequest(

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be SCREAMING_SNAKE_CASE, e.g. CLAIMS_READ")
        String name,

        @Size(max = 255)
        String description,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be SCREAMING_SNAKE_CASE, e.g. CLAIMS")
        String resource) {
}
