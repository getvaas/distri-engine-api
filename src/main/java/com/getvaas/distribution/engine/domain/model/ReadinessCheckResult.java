package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

public record ReadinessCheckResult(
        ReadinessCheckType type,
        ReadinessCheckStatus status,
        String reason
) {}
