package in.gov.ipie.service.iam.command;

import java.util.Set;

public record CreateRoleCommand(String name, String description, Set<String> permissionNames) {

    /** Defensive copy - a caller must not be able to mutate this command's state after construction via the original set reference. */
    public CreateRoleCommand {
        permissionNames = permissionNames == null ? null : Set.copyOf(permissionNames);
    }
}
