package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipCrossValidationConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipSourceConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipCrossValidationRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipSourceRequest;
import org.springframework.stereotype.Component;

/**
 * Construye la etapa Ownership — Source (VPR-9635) y Cross Validation (VPR-9636). Ambas
 * sub-secciones son opcionales: un deal puede no tener ninguna configurada todavía.
 */
@Component
public class OwnershipConfigBuilder {

    public OwnershipConfig build(UpdateOwnershipRequest request) {
        var source = buildSourceConfig(request.source());
        var crossValidation = buildCrossValidationConfig(request.crossValidation());
        return new OwnershipConfig(source, crossValidation);
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
