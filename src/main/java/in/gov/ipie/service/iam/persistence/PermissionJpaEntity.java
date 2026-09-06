package in.gov.ipie.service.iam.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;

/**
 * The permission catalogue. Mostly seeded via Flyway - every permission the code enforces
 * originates in a migration - with a SUPER_ADMIN-gated create path for preparing new entries, see
 * {@code PermissionRepository}'s Javadoc. Standard audit + soft-delete columns are inherited from
 * {@link AuditableJpaEntity}; the seeding migrations supply them directly (master standards doc,
 * 7.2), while rows written through JPA get them from the auditing listener.
 */
@Entity
@Table(name = "permissions")
public class PermissionJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String resource;

    protected PermissionJpaEntity() {
        // required by JPA
    }

    /** New-permission constructor - {@code id} is assigned by {@link GeneratedValue} on persist. */
    public PermissionJpaEntity(String name, String description, String resource) {
        this.name = name;
        this.description = description;
        this.resource = resource;
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

    public String getResource() {
        return resource;
    }
}
