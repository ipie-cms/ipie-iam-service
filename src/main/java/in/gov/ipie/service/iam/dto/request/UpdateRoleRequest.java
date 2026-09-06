package in.gov.ipie.service.iam.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Carries no name field: a role's name is fixed once created, because it is the identifier every
 * issued JWT and permission check is keyed on. See {@code Role#update}.
 */
public record UpdateRoleRequest(
        @Size(max = 255)
        String description,

        @NotEmpty
        Set<String> permissionNames) {

    public UpdateRoleRequest {
        permissionNames = permissionNames == null ? null : Set.copyOf(permissionNames);
    }
}
