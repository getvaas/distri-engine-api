package com.getvaas.distribution.engine.domain.model;

/**
 * Resultado del "proceso base" de distribución (VPR-9662): readiness checks + resolución del pool
 * de fondos elegibles, ya particionado entre distribuibles y ownerless (VPR-9667). {@code funds}
 * queda vacío (ambas listas) si {@code readiness.readyToDistribute()} es {@code false} — no tiene
 * sentido resolver el pool si no se puede distribuir. No modela todavía assignments, persistencia
 * ni notificaciones — esos pasos no existen aún en el motor.
 */
public record DistributionExecutionResult(
        ReadinessCheckOutcome readiness,
        PartitionedPoolFunds funds
) {}
