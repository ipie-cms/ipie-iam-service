package in.gov.ipie.service.iam.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(

        @NotBlank
        @Size(max = 50)
        String name,

        @Size(max = 255)
        String description,

        @NotEmpty
        Set<String> permissionNames) {

    /** Defensive copy - a caller must not be able to mutate this request's state after construction via the original set reference. */
    public CreateRoleRequest {
        permissionNames = permissionNames == null ? null : Set.copyOf(permissionNames);
    }
}
