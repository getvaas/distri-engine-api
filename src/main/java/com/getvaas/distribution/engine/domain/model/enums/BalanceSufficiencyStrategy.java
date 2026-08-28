package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Qué hacer si el balance disponible no alcanza para el monto a distribuir (VPR-9703).
 */
public enum BalanceSufficiencyStrategy {
    SUFFICIENT_OR_STOP,
    UNTIL_EXHAUSTED,
    SKIP_IF_INSUFFICIENT,
    IGNORE_BALANCE
}
