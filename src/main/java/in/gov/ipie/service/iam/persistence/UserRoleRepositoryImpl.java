package in.gov.ipie.service.iam.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.repository.UserRoleRepository;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class UserRoleRepositoryImpl implements UserRoleRepository {

    private final UserRoleJpaRepository jpaRepository;

    @Override
    public void assign(UUID userId, UUID keycloakUserId, UUID roleId, String assignedBy) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (jpaRepository.existsById(id)) {
            return;
        }
        jpaRepository.save(new UserRoleJpaEntity(id, keycloakUserId, Instant.now(), assignedBy));
    }

    @Override
    public List<UUID> findRoleIdsByKeycloakUserId(UUID keycloakUserId) {
        return jpaRepository.findByKeycloakUserId(keycloakUserId).stream().map(entity -> entity.getId().getRoleId()).toList();
    }

    @Override
    public List<UUID> findRoleIdsByUserId(UUID userId) {
        return jpaRepository.findByIdUserId(userId).stream().map(entity -> entity.getId().getRoleId()).toList();
    }

    @Override
    public void revoke(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (!jpaRepository.existsById(id)) {
            return;
        }
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByRoleId(UUID roleId) {
        return jpaRepository.existsByIdRoleId(roleId);
    }
}
