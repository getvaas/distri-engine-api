package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;

import java.util.List;

public record ReadinessCheckOutcome(
        boolean readyToDistribute,
        List<ReadinessCheckResult> results
) {
    public static ReadinessCheckOutcome of(List<ReadinessCheckResult> results) {
        boolean ready = results.stream().noneMatch(r -> r.status() == ReadinessCheckStatus.FAILED);
        return new ReadinessCheckOutcome(ready, results);
    }
}
