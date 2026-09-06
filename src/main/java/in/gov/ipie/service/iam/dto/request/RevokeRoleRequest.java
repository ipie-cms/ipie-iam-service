package in.gov.ipie.service.iam.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Mirrors {@link AssignRoleRequest} - same identifiers, opposite effect. */
public record RevokeRoleRequest(
        @NotNull
        UUID keycloakUserId,

        @NotBlank
        String roleName,

        String comment) {
}
