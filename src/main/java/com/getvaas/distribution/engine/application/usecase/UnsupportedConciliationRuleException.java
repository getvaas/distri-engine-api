package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ConciliationRequirementRule;

/**
 * {@code ConciliationToleranceCheck} solo puede evaluar reglas cuyo par de tablas incluya
 * PAYMENT_TAPE y se apoye en una FK real ya mapeada ({@code payment_id} para PAYMENTS,
 * {@code fund_transfer_id} para FUNDS_TRANSFER), y solo reglas sin {@code gateway} específico (no
 * hay dato de gateway en {@code payment_tape} para filtrar por él). Cualquier otra combinación
 * configurada falla explícito en vez de evaluarse mal o ignorarse en silencio.
 */
public class UnsupportedConciliationRuleException extends RuntimeException {

    public UnsupportedConciliationRuleException(ConciliationRequirementRule rule) {
        super("La regla de conciliación " + rule.tableA() + " vs " + rule.tableB()
                + (rule.gateway() != null ? " (gateway=" + rule.gateway() + ")" : "")
                + " no está soportada todavía por ConciliationToleranceCheck — solo se soportan "
                + "PAYMENT_TAPE vs PAYMENTS y PAYMENT_TAPE vs FUNDS_TRANSFER, sin gateway específico");
    }
}
