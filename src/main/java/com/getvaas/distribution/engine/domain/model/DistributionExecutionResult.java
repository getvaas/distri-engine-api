package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Resultado del "proceso base" de distribución (VPR-9662): readiness checks + resolución del pool
 * de fondos elegibles. {@code funds} queda vacía si {@code readiness.readyToDistribute()} es
 * {@code false} — no tiene sentido resolver el pool si no se puede distribuir. No modela todavía
 * ownership, assignments, persistencia ni notificaciones — esos pasos no existen aún en el motor.
 */
public record DistributionExecutionResult(
        ReadinessCheckOutcome readiness,
        List<PoolFund> funds
) {}
