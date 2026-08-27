package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configura la etapa Readiness Checks (VPR-9661). Los 3 checks se pueden habilitar por config aunque
 * hoy solo BUSINESS_DAY tenga una implementación real en el motor de ejecución (ver
 * {@code ReadinessCheckRunner}) — habilitar los otros 2 no falla, simplemente todavía no hacen nada.
 */
@Component
@RequiredArgsConstructor
public class UpdateReadinessChecksConfigUseCase {

    private static final List<ReadinessCheckType> DEFAULT_ENABLED_CHECKS = List.of(
            ReadinessCheckType.PAYMENT_TAPE_LOADED,
            ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
            ReadinessCheckType.BUSINESS_DAY
    );
    private static final ReadinessCheckFailureAction DEFAULT_FAILURE_ACTION = ReadinessCheckFailureAction.PAUSE_AND_ALERT;
    private static final ReadinessCheckRetry DEFAULT_RETRY = ReadinessCheckRetry.NEXT_CYCLE;

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateReadinessChecksConfigRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var readinessChecksConfig = new ReadinessChecksConfig(
                request.enabledChecks() != null ? request.enabledChecks() : DEFAULT_ENABLED_CHECKS,
                request.failureAction() != null ? request.failureAction() : DEFAULT_FAILURE_ACTION,
                request.retry() != null ? request.retry() : DEFAULT_RETRY
        );

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                readinessChecksConfig,
                existing.config().notifications()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
