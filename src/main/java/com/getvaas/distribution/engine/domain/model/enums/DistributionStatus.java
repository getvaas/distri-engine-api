package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Valores reales de {@code distribution.status} en {@code master_trust_servicer} (verificado contra
 * {@code DistributionStatus.kt} del sistema real) — este motor persiste directo en esa misma tabla,
 * así que los valores tienen que coincidir exacto para que cualquier lectura del sistema real los
 * reconozca. Se persiste como {@code name().toLowerCase()} (matchea 1:1 los valores reales, todos en
 * snake_case minúscula). Solo {@link #APPROVED}, {@link #NOTHING_DISTRIBUTABLE} y {@link #DRAFT} son
 * alcanzables desde este motor hoy — el resto ({@link #NOT_ENABLED}, {@link #CANNOT_DISTRIBUTE},
 * {@link #ERROR}) existen en el sistema real pero no los produce ningún caso de este motor todavía.
 */
public enum DistributionStatus {
    APPROVED,
    NOT_ENABLED,
    CANNOT_DISTRIBUTE,
    NOTHING_DISTRIBUTABLE,
    DRAFT,
    ERROR;

    public String dbValue() {
        return name().toLowerCase();
    }
}
