package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Qué hacer si el balance disponible no alcanza para el monto a distribuir (VPR-9703). Valores
 * verificados 1:1 contra el enum real {@code BalanceStrategy} de {@code master-trust-servicer-api}
 * — no incluye {@code UNTIL_BALANCE_EXHAUSTED} (deprecado y muerto en el código real, su call site
 * está comentado) ni ningún valor "sin chequeo": eso se modela con {@code sufficiencyStrategy=null},
 * mismo patrón que {@code distributionStrategy=null} para {@code DEFAULT}.
 */
public enum BalanceSufficiencyStrategy {
    SUFFICIENT_BALANCE_OR_STOP,
    SUFFICIENT_BALANCE_OR_SKIP_ALL_BORROWERS,
    UNTIL_BALANCE_EXHAUSTED_WHILE_FITTING_PAYMENTS
}
