package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckSetting;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.web.dto.ReadinessCheckSettingRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Construye la etapa Readiness Checks — preconditions (VPR-9637) y failure behavior (VPR-9638).
 * Cada check habilitado tiene su propio {@code failureAction}/{@code retry}, no uno global para
 * toda la config — los borrowers reales usan los 3 modos (bloquea-todo, particiona-y-sigue,
 * solo-reporta), no es válido simplificar a uno solo por default.
 */
@Component
public class ReadinessChecksConfigBuilder {

    private static final List<ReadinessCheckType> DEFAULT_ENABLED_CHECK_TYPES = List.of(
            ReadinessCheckType.PAYMENT_TAPE_LOADED,
            ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
            ReadinessCheckType.BUSINESS_DAY
    );
    private static final ReadinessCheckFailureAction DEFAULT_FAILURE_ACTION = ReadinessCheckFailureAction.PAUSE_AND_ALERT;
    private static final ReadinessCheckRetry DEFAULT_RETRY = ReadinessCheckRetry.NEXT_CYCLE;

    public ReadinessChecksConfig build(UpdateReadinessChecksConfigRequest request) {
        var settingRequests = request.checks();
        if (settingRequests == null || settingRequests.isEmpty()) {
            var defaults = DEFAULT_ENABLED_CHECK_TYPES.stream()
                    .map(type -> new ReadinessCheckSetting(type, DEFAULT_FAILURE_ACTION, DEFAULT_RETRY, null))
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

        return new ReadinessCheckSetting(
                settingRequest.type(), failureAction, retry, settingRequest.forceRunOnNonBusinessDay());
    }
}
