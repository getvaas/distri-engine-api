package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Cómo se calcula el monto a distribuir hacia el owner (VPR-9703). {@code DEFAULT} no usa
 * {@code distributionValue} (suma la columna de monto agrupada por owner, sin cálculo especial);
 * el resto de los valores sí lo usa, con un significado distinto cada uno (peso, porcentaje o
 * monto fijo).
 */
public enum AmountDistributionStrategy {
    DEFAULT,
    PROPORTIONAL_WEIGHT,
    PERCENTAGE_OF_POOL,
    PERCENTAGE_OF_REMAINING,
    FIXED_AMOUNT
}
