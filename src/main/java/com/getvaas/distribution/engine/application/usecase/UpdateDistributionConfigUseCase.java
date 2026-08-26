package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Actualiza únicamente los campos de Deal Info (VPR-9644). Las demás secciones del config_json
 * (pool, paymentFilters, virtualColumns, rules, ownership, readinessChecks, notifications)
 * se preservan tal cual estaban — todavía no tienen ticket propio de edición.
 */
@Component
@RequiredArgsConstructor
public class UpdateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateDistributionConfigRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.masterTrustId() != null) {
            entity.setMasterTrustId(request.masterTrustId());
        }

        var updatedPayload = new DistributionConfigPayload(
                request.country() != null ? request.country() : existing.config().country(),
                request.currency() != null ? request.currency() : existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                existing.config().readinessChecks(),
                existing.config().notifications()
        );
        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
