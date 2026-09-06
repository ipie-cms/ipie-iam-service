package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import in.gov.ipie.common.persistence.AuditableJpaEntity;

/**
 * {@code assigned_at}/{@code assigned_by} stay as their own domain-named columns (a role
 * assignment's creation, not a generic update) alongside the inherited standard audit +
 * soft-delete columns from {@link AuditableJpaEntity} - {@code is_active}/{@code deleted_at}/
 * {@code deleted_by} are what let a role assignment be revoked without deleting the row.
 */
@Entity
@Table(name = "user_roles")
public class UserRoleJpaEntity extends AuditableJpaEntity {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    protected UserRoleJpaEntity() {
        // required by JPA
    }

    public UserRoleJpaEntity(UserRoleId id, UUID keycloakUserId, Instant assignedAt, String assignedBy) {
        this.id = id;
        this.keycloakUserId = keycloakUserId;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public UserRoleId getId() {
        return id;
    }

    public UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }
}
