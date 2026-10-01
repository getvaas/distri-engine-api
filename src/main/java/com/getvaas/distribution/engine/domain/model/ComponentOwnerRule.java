package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.PaymentType;

import java.util.List;

/**
 * Distribution Rules — una regla de la cascada de pagos, a quién le corresponde qué (VPR-9643,
 * VPR-9698). {@code owner} es un identificador libre por ahora — resolverlo contra cuentas/partes
 * reales es responsabilidad de Ownership (VPR-9635/9636, todavía sin construir), no de esta regla.
 * <p>
 * {@code component} ({@code PaymentComponent}) se eliminó (VPR-9698) — era un identificador inerte
 * en ejecución (nunca leído por {@code CalculateAssignmentsUseCase}), y su validación de
 * único/requerido forzaba un tope real de 4 reglas que no corresponde al diseño real (cascada
 * arbitraria de reglas por owner, ver mockups). {@code componentOwners} pasa a ser una lista de
 * largo arbitrario, ejecutada en el orden ya real de {@code CalculateAssignmentsUseCase} (reglas
 * basadas en pool → {@code PERCENTAGE_OF_REMAINING} en cascada → una única {@code DEFAULT} de
 * cierre). Para atribuir el monto real de un componente de cuota (principal/interés/etc.) se usa la
 * estrategia {@code SUM_COLUMN} sobre una columna real del payment tape (ver
 * {@code BalanceStrategyConfig.amountField}), no este campo.
 * <p>
 * {@code balanceStrategy} (VPR-9703) es opcional — no todas las reglas necesitan tener la
 * estrategia de balance definida todavía.
 * <p>
 * {@code distributeAccountingPayments} (VPR-9706) es un override independiente por regla, sin
 * relación forzada con el flag equivalente a nivel deal
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
 * {@code fromAccountId} (VPR-9698) es análogo, del lado origen — config-only por ahora, sin uso ni
 * validación en ejecución todavía.
 * <p>
 * {@code paymentTypes} (VPR-9698) declara con qué tipos de pago (CASH/ACCT) trabaja esta regla —
 * config-only, su efecto en ejecución queda sin definir todavía (fuera de alcance de VPR-9698).
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}: una config guardada antes de VPR-9698 puede
 * tener {@code "component"} persistido en su {@code config_json} — sin esto, deserializarla
 * revienta con {@code UnrecognizedPropertyException} en vez de simplemente ignorar el campo
 * sobrante (confirmado: rompía {@code GET /configs} sobre datos reales ya persistidos).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComponentOwnerRule(
        String owner,
        String description,
        BalanceStrategyConfig balanceStrategy,
        boolean distributeAccountingPayments,
        Long toAccountId,
        Long fromAccountId,
        List<PaymentType> paymentTypes
) {}
