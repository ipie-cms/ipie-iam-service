package in.gov.ipie.service.iam.command;

import java.util.Set;
import java.util.UUID;

/**
 * An administrator's edit to an existing role. Carries no name: see {@code Role#update} for why a
 * role's name is fixed once created.
 */
public record UpdateRoleCommand(UUID roleId, String description, Set<String> permissionNames) {

    public UpdateRoleCommand {
        permissionNames = permissionNames == null ? Set.of() : Set.copyOf(permissionNames);
    }
}
