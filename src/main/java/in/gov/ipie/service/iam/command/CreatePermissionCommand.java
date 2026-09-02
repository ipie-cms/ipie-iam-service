package in.gov.ipie.service.iam.command;

/**
 * Creates a catalogue entry an administrator can then compose a role from. {@code resource} is the
 * ABAC axis the permission applies to and is what the admin UI groups the catalogue by, so it is
 * required - an ungrouped permission is one an administrator cannot find.
 */
public record CreatePermissionCommand(String name, String description, String resource) {
}
