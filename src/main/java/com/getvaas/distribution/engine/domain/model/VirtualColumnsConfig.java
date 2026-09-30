package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Etapa 4 — Virtual Columns (VPR-9696). Columnas derivadas por fórmula sobre el payment tape,
 * evaluadas por fila antes de correr Distribution Rules/Ownership/Payment Filters. Distribution
 * Rules (VPR-9643, VPR-9698) puede referenciar una virtual column por nombre desde
 * {@code BalanceStrategyConfig.amountField} (estrategia {@code SUM_COLUMN}), igual que ya
 * referencia columnas reales — el evaluador de fórmulas en tiempo de ejecución todavía no existe
 * (ver `docs/architecture/distribution-config-schema.md`). Se permite anidamiento libre entre
 * virtual columns (una fórmula puede referenciar a otra virtual column ya definida), sin validar
 * orden de evaluación ni ciclos en esta etapa de configuración.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VirtualColumnsConfig(
        List<VirtualColumn> columns
) {}
