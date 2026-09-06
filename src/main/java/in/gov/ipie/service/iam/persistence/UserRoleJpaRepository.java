package in.gov.ipie.service.iam.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleId> {

    List<UserRoleJpaEntity> findByKeycloakUserId(UUID keycloakUserId);

    List<UserRoleJpaEntity> findByIdUserId(UUID userId);

    boolean existsByIdRoleId(UUID roleId);
}
