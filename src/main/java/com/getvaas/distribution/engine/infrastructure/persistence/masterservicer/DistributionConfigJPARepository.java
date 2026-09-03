package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributionConfigJPARepository extends JpaRepository<DistributionEngineConfigEntity, String> {

    Optional<DistributionEngineConfigEntity> findByIdAndActiveTrue(String id);

    List<DistributionEngineConfigEntity> findByCompanyIdAndStatusAndActiveTrue(
            Long companyId, DistributionConfigStatus status);
}
