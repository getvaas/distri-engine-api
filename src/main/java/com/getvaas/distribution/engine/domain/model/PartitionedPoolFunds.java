package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Paso 6 del pipeline de ejecución (VPR-9667): el pool ya resuelto, particionado entre fondos con
 * owner real ({@code distributable}) y fondos sin owner resuelto ({@code ownerless}). Reemplaza el
 * {@code p.ownerName!!} del motor real (NPE si el owner es null, tumba toda la corrida) — acá el
 * owner nunca es {@code null} (ver {@code ResolveOwnershipUseCase}), así que la partición nunca
 * falla, solo clasifica.
 */
public record PartitionedPoolFunds(
        List<PoolFund> distributable,
        List<PoolFund> ownerless
) {}
