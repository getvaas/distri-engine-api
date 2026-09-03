package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface MasterServicerDistributionJPARepository extends JpaRepository<MasterServicerDistributionEntity, Long> {

    boolean existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
            Long masterTrustServicerId, LocalDateTime fromDate, LocalDateTime untilDate);
}
