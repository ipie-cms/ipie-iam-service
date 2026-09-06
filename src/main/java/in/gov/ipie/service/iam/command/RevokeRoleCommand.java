package in.gov.ipie.service.iam.command;

import java.util.UUID;

/**
 * Withdraws a previously assigned role. Mirrors {@link AssignRoleCommand} field for field so the
 * two read as a pair, including {@code comment} - a revocation is the half of the story an audit
 * trail most often needs a reason for.
 */
public record RevokeRoleCommand(UUID userId, UUID keycloakUserId, String roleName, String comment) {
}
