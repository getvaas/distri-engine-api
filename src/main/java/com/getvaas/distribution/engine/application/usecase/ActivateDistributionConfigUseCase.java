package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Activa una config, garantizando que nunca haya dos ACTIVE para el mismo borrower a la vez
 * (decisión resuelta en VPR-9644, implementada acá).
 */
@Component
@RequiredArgsConstructor
public class ActivateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));

        var now = LocalDateTime.now();
        var currentlyActive = repository.findByCompanyIdAndStatusAndDeletedFalse(
                entity.getCompanyId(), DistributionConfigStatus.ACTIVE);
        for (var sibling : currentlyActive) {
            if (!sibling.getId().equals(id)) {
                sibling.setStatus(DistributionConfigStatus.INACTIVE);
                sibling.setUpdatedAt(now);
                repository.save(sibling);
            }
        }

        entity.setStatus(DistributionConfigStatus.ACTIVE);
        entity.setUpdatedAt(now);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
