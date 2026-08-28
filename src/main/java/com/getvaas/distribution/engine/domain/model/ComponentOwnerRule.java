package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;

/**
 * Distribution Rules — a quién le corresponde un componente de la cuota (VPR-9643).
 * {@code owner} es un identificador libre por ahora — resolverlo contra cuentas/partes reales es
 * responsabilidad de Ownership (VPR-9635/9636, todavía sin construir), no de esta regla.
 * <p>
 * Verificado contra el código real (`SistecreditoDistributor.kt`): los 4 valores de
 * {@link PaymentComponent} mapean 1:1 contra columnas reales de {@code PaymentTapeEntity}
 * ({@code current_principal}, {@code current_interest}, {@code moratory_interest},
 * {@code current_guarantee}) — hoy no existe ningún split real por componente en producción, solo
 * una resta escalar única de {@code currentGuarantee} sobre {@code netAmount}.
 * <p>
 * {@code balanceStrategy} (VPR-9703) es opcional — no todas las reglas necesitan tener la
 * estrategia de balance definida todavía.
 */
public record ComponentOwnerRule(
        PaymentComponent component,
        String owner,
        String description,
        BalanceStrategyConfig balanceStrategy
) {}
