package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Regla de transferencia de balance entre cuentas, dentro de una {@link BalanceStrategyConfig}
 * (VPR-9702). Cada cuenta se identifica por {@code accountId: Long}, mismo patrón que
 * {@code AccountBalanceSource.accountId} (Pool Strategy, VPR-9629).
 * <p>
 * {@code condition} es opcional — una sola condición por regla (no un builder de grupos OR/AND
 * como Payment Filters), reusando directamente {@link PaymentFilterCondition} (VPR-9631). Sin
 * validación cruzada entre {@code fromAccountIds} y {@code toAccountIds}.
 * <p>
 * {@code deductions} (VPR-9704) declara las comisiones a descontar de esta transferencia — puede
 * haber varias distintas bajo la misma regla.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountTransferRule(
        List<Long> fromAccountIds,
        List<Long> toAccountIds,
        PaymentFilterCondition condition,
        List<Deduction> deductions
) {}
