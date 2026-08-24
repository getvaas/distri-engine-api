package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckOutcome;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;

import java.util.List;

public record ReadinessCheckOutcomeResponse(
        boolean readyToDistribute,
        List<ReadinessCheckResult> results
) {
    public static ReadinessCheckOutcomeResponse from(ReadinessCheckOutcome outcome) {
        return new ReadinessCheckOutcomeResponse(outcome.readyToDistribute(), outcome.results());
    }
}
