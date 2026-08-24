package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Payment Filters — Accounting Payments (VPR-9631). Identifica pagos contables (sin respaldo de
 * caja) mediante {@code conditionGroups} (OR entre grupos, AND dentro de cada grupo) y decide si se
 * distribuyen igual ({@code distributeAccountingPayments}). Cuando {@code hasAccountingPayments} es
 * {@code false}, no hay pagos contables que identificar: {@code distributeAccountingPayments} queda
 * forzado en {@code true} y {@code conditionGroups} vacío, sin importar lo que se haya enviado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountingPaymentsConfig(
        boolean hasAccountingPayments,
        boolean distributeAccountingPayments,
        List<PaymentFilterConditionGroup> conditionGroups
) {}
