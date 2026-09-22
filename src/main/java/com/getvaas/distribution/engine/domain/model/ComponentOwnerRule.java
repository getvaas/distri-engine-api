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
 * <p>
 * {@code distributeAccountingPayments} (VPR-9706) es un override independiente por componente,
 * sin relación forzada con el flag equivalente a nivel deal
 * ({@code AccountingPaymentsConfig.distributeAccountingPayments}, VPR-9631) — comparten nombre
 * por describir el mismo concepto de negocio, pero se evalúan por separado en ejecución.
 * <p>
 * {@code toAccountId} (VPR-9668) es la cuenta real a la que se transfiere el monto de esta regla —
 * verificado contra {@code AssignmentConfig.toAccountId}/{@code Assignment.accountId} del motor
 * real: los deals reales configuran este id a mano, {@code owner} es solo una etiqueta descriptiva
 * al lado (mismo patrón que {@code owner_name}/{@code template_code} en la config real), no una
 * clave de la que se pueda derivar el id — no existe ningún lookup dinámico owner→cuenta en el
 * sistema real. Opcional al guardar (permite drafts parciales, mismo criterio del resto del
 * repo), pero requerido en tiempo de ejecución para poder persistir un {@code Assignment} real.
 */
public record ComponentOwnerRule(
        PaymentComponent component,
        String owner,
        String description,
        BalanceStrategyConfig balanceStrategy,
        boolean distributeAccountingPayments,
        Long toAccountId
) {}
