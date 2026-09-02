package in.gov.ipie.service.iam.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    boolean existsByName(String name);

    Optional<RoleJpaEntity> findByName(String name);
}
