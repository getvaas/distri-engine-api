package com.getvaas.distribution.engine.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * {@code date} es opcional — {@code null} significa "hoy" (ver
 * {@code DistributionExecutionRouter}), no una corrida sin fecha.
 */
public record RunDistributionRequest(
        @NotNull Long companyId,
        LocalDate date
) {}
