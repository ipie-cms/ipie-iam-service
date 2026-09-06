package in.gov.ipie.service.iam.dto.response;

import java.util.Set;

public record RoleResponse(String id, String name, String description, Set<String> permissionNames) {

    /** Defensive copy - callers must not be able to mutate this response's state via the original set reference. */
    public RoleResponse {
        permissionNames = permissionNames == null ? null : Set.copyOf(permissionNames);
    }
}
