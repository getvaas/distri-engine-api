package com.getvaas.distribution.engine.infrastructure.persistence.payments;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributionConfigJPARepository extends JpaRepository<DistributionEngineConfigEntity, String> {

    Optional<DistributionEngineConfigEntity> findByIdAndDeletedFalse(String id);

    List<DistributionEngineConfigEntity> findByCompanyIdAndStatusAndDeletedFalse(
            Long companyId, DistributionConfigStatus status);
}
