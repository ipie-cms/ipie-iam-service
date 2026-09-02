package in.gov.ipie.service.iam.command;

import java.util.UUID;

public record AssignRoleCommand(UUID userId, UUID keycloakUserId, String roleName, String comment) {
}
