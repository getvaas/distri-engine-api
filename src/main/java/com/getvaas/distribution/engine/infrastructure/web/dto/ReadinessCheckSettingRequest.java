package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

public record ReadinessCheckSettingRequest(
        ReadinessCheckType type,
        ReadinessCheckFailureAction failureAction,
        ReadinessCheckRetry retry,
        Boolean forceRunOnNonBusinessDay
) {}
