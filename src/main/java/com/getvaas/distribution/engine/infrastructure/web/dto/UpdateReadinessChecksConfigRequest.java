package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

import java.util.List;

public record UpdateReadinessChecksConfigRequest(
        List<ReadinessCheckType> enabledChecks,
        ReadinessCheckFailureAction failureAction,
        ReadinessCheckRetry retry
) {}
