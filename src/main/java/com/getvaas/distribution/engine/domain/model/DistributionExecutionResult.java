package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Resultado del pipeline de ejecución: readiness checks + resolución del pool de fondos elegibles
 * ya particionado entre distribuibles y ownerless (VPR-9667), los assignments por regla (VPR-9668)
 * y, si se llegó a persistir, el id real de la distribución grabada (VPR-9669) — {@code null} si
 * {@code readiness.readyToDistribute()} es {@code false} (no hay nada que persistir). No modela
 * todavía notificaciones ni el reporte distribuido/no-distribuido.
 */
public record DistributionExecutionResult(
        ReadinessCheckOutcome readiness,
        PartitionedPoolFunds funds,
        List<Assignment> assignments,
        Long distributionId
) {}
