package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Desactiva una config (VPR-9641) — contraparte directa de
 * {@link ActivateDistributionConfigUseCase}. Sin validar el status actual, sin lógica de
 * "hermanos": desactivar una config no afecta a ninguna otra (esa garantía es específica de
 * activate, VPR-9644).
 */
@Component
@RequiredArgsConstructor
public class DeactivateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));

        entity.setStatus(DistributionConfigStatus.INACTIVE);
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
