package in.gov.ipie.service.iam.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(

        @NotNull
        UUID keycloakUserId,

        @NotBlank
        String roleName,

        /**
         * The reason for this assignment, for the audit trail. Optional here - a human-facing UI
         * is expected to require it before submitting (see {@code Auditable}'s Javadoc).
         */
        String comment) {
}
