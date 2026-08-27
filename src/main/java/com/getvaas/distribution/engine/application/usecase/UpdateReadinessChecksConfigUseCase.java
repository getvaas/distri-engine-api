package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckSetting;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.ReadinessCheckSettingRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Configura la etapa Readiness Checks — preconditions (VPR-9637) y failure behavior (VPR-9638).
 * Cada check habilitado tiene su propio {@code failureAction}/{@code retry}, no uno global para
 * toda la config — los borrowers reales usan los 3 modos (bloquea-todo, particiona-y-sigue,
 * solo-reporta), no es válido simplificar a uno solo por default.
 */
@Component
@RequiredArgsConstructor
public class UpdateReadinessChecksConfigUseCase {

    private static final List<ReadinessCheckType> DEFAULT_ENABLED_CHECK_TYPES = List.of(
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

        var readinessChecksConfig = buildReadinessChecksConfig(request.checks());

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                readinessChecksConfig,
                existing.config().notifications(),
                existing.config().transferInstructions()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private ReadinessChecksConfig buildReadinessChecksConfig(List<ReadinessCheckSettingRequest> settingRequests) {
        if (settingRequests == null || settingRequests.isEmpty()) {
            var defaults = DEFAULT_ENABLED_CHECK_TYPES.stream()
                    .map(type -> new ReadinessCheckSetting(type, DEFAULT_FAILURE_ACTION, DEFAULT_RETRY))
                    .toList();
            return new ReadinessChecksConfig(defaults);
        }

        var seenTypes = new HashSet<ReadinessCheckType>();
        var checks = settingRequests.stream()
                .map(r -> buildReadinessCheckSetting(r, seenTypes))
                .toList();

        return new ReadinessChecksConfig(checks);
    }

    private ReadinessCheckSetting buildReadinessCheckSetting(
            ReadinessCheckSettingRequest settingRequest, HashSet<ReadinessCheckType> seenTypes) {
        if (settingRequest.type() == null) {
            throw new InvalidDistributionConfigException("cada check requiere 'type'");
        }
        if (!seenTypes.add(settingRequest.type())) {
            throw new InvalidDistributionConfigException(
                    "el check " + settingRequest.type() + " está repetido en 'checks'");
        }

        var failureAction = settingRequest.failureAction() != null ? settingRequest.failureAction() : DEFAULT_FAILURE_ACTION;
        var retry = settingRequest.retry() != null ? settingRequest.retry() : DEFAULT_RETRY;

        return new ReadinessCheckSetting(settingRequest.type(), failureAction, retry);
    }
}
