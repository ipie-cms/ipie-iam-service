package in.gov.ipie.service.iam.persistence;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;

/**
 * JPA representation of a Role. Never returned from an API and never referenced outside the
 * infrastructure layer - {@code RolePersistenceMapper} converts to/from the domain {@code Role}.
 * Standard audit + soft-delete columns are inherited from {@link AuditableJpaEntity}.
 */
@Entity
@Table(name = "roles")
public class RoleJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionJpaEntity> permissions = new HashSet<>();

    protected RoleJpaEntity() {
        // required by JPA
    }

    public RoleJpaEntity(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** Mutable - the persistence mapper populates this from resolved {@link PermissionJpaEntity} lookups. */
    public void setDescription(String description) {
        this.description = description;
    }

    public Set<PermissionJpaEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionJpaEntity> permissions) {
        this.permissions = permissions;
    }
}
