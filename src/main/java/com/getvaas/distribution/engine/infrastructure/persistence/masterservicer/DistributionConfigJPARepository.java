package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DistributionConfigJPARepository extends
        JpaRepository<DistributionEngineConfigEntity, String>,
        JpaSpecificationExecutor<DistributionEngineConfigEntity> {

    Optional<DistributionEngineConfigEntity> findByIdAndActiveTrue(String id);

    List<DistributionEngineConfigEntity> findByCompanyIdAndStatusAndActiveTrue(
            Long companyId, DistributionConfigStatus status);
}
