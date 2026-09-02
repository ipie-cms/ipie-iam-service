package in.gov.ipie.service.iam.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PillarResolutionJpaRepository extends JpaRepository<PillarResolutionEntity, UUID> {

    Optional<PillarResolutionEntity> findByPillarTypeAndExternalPillarId(
            String pillarType, String externalPillarId);

    @Modifying
    @Query(
            "delete from PillarResolutionEntity e where e.pillarType = :pillarType "
                    + "and e.externalPillarId = :externalPillarId")
    void deleteByPillarTypeAndExternalPillarId(
            @Param("pillarType") String pillarType, @Param("externalPillarId") String externalPillarId);
}
