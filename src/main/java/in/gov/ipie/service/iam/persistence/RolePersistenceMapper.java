package in.gov.ipie.service.iam.persistence;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.iam.domain.Role;

/** Converts between the JPA entity and the domain model, the same way {@code UserPersistenceMapper} does elsewhere. */
@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        Set<String> permissionNames = entity.getPermissions().stream().map(PermissionJpaEntity::getName).collect(Collectors.toSet());
        return new Role(entity.getId(), entity.getName(), entity.getDescription(), permissionNames, auditMetadata);
    }

    public RoleJpaEntity toNewEntity(Role role) {
        return new RoleJpaEntity(role.getId(), role.getName(), role.getDescription());
    }
}
