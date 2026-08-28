package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipCrossValidationConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipSourceConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipCrossValidationRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipSourceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Configura la etapa Ownership — Source (VPR-9635) y Cross Validation (VPR-9636). Ambas
 * sub-secciones son opcionales: un deal puede no tener ninguna configurada todavía.
 */
@Component
@RequiredArgsConstructor
public class UpdateOwnershipUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateOwnershipRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var source = buildSourceConfig(request.source());
        var crossValidation = buildCrossValidationConfig(request.crossValidation());
        var ownership = new OwnershipConfig(source, crossValidation);

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                ownership,
                existing.config().readinessChecks(),
                existing.config().notifications(),
                existing.config().transferInstructions()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private OwnershipSourceConfig buildSourceConfig(UpdateOwnershipSourceRequest request) {
        if (request == null) {
            return null;
        }
        if (request.sourceType() == null) {
            throw new InvalidDistributionConfigException("'source' requiere 'sourceType'");
        }
        if (request.field() == null || request.field().isBlank()) {
            throw new InvalidDistributionConfigException("'source' requiere 'field'");
        }

        return new OwnershipSourceConfig(request.sourceType(), request.field(), request.defaultOwner());
    }

    private OwnershipCrossValidationConfig buildCrossValidationConfig(UpdateOwnershipCrossValidationRequest request) {
        if (request == null) {
            return null;
        }

        var enabled = Boolean.TRUE.equals(request.enabled());
        if (!enabled) {
            return new OwnershipCrossValidationConfig(false, null);
        }

        if (request.mismatchStrategy() == null) {
            throw new InvalidDistributionConfigException("'crossValidation' con enabled=true requiere 'mismatchStrategy'");
        }

        return new OwnershipCrossValidationConfig(true, request.mismatchStrategy());
    }
}
