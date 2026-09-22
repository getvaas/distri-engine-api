package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Resultado del "proceso base" de distribución: readiness checks + resolución del pool de fondos
 * elegibles ya particionado entre distribuibles y ownerless (VPR-9667), y los assignments por
 * regla ya calculados sobre el pool distribuible (VPR-9668). {@code funds} y {@code assignments}
 * quedan vacíos si {@code readiness.readyToDistribute()} es {@code false} — no tiene sentido
 * resolver el pool ni armar assignments si no se puede distribuir. No modela todavía persistencia
 * ni notificaciones — esos pasos no existen aún en el motor.
 */
public record DistributionExecutionResult(
        ReadinessCheckOutcome readiness,
        PartitionedPoolFunds funds,
        List<Assignment> assignments
) {}
