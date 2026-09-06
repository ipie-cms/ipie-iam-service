package in.gov.ipie.service.iam.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserCredentialJpaRepository extends JpaRepository<UserCredentialJpaEntity, UUID> {
}
