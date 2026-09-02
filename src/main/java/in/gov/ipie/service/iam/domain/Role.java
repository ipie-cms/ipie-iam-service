package in.gov.ipie.service.iam.domain;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * The Role domain model - deliberately independent of the JPA entity in the infrastructure layer
 * (master standards doc, section 16). Permissions are tracked by name ({@code
 * permissions.resource} is the ABAC attribute axis, looked up separately when needed) rather than
 * as a rich nested aggregate - no requirement yet calls for permissions to be managed except by
 * name at role-creation time.
 */
@Getter
public final class Role {

    private final UUID id;
    private String name;
    private String description;
    private Set<String> permissionNames;
    private final AuditMetadata auditMetadata;

    public Role(UUID id, String name, String description, Set<String> permissionNames, AuditMetadata auditMetadata) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissionNames = Set.copyOf(permissionNames);
        this.auditMetadata = auditMetadata;
    }

    public static Role createNew(String name, String description, Set<String> permissionNames) {
        return new Role(null, name, description, permissionNames, null);
    }

    /**
     * Applies an administrator's edit to this role. The name is deliberately not editable: it is
     * the identifier the realm role, every issued JWT and every {@code @RequiresPermission} check
     * are keyed on, so renaming would silently strip access from everyone currently holding it.
     * A rename is a create-and-reassign, not an update.
     */
    public void update(String description, Set<String> permissionNames) {
        this.description = description;
        this.permissionNames = Set.copyOf(permissionNames);
    }
}
