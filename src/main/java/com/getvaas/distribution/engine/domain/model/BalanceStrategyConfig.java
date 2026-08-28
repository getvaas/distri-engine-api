package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estrategia de balance de una {@link ComponentOwnerRule} (VPR-9703) — cómo mover el balance de
 * un owner hacia su cuenta. {@code amountField} sigue el mismo patrón que
 * {@code PaymentTapePoolConfig.amountField} (VPR-9628): string libre, cualquier columna real de
 * {@code payment_tape}, no un enum cerrado.
 * <p>
 * {@code distributionValue} es un campo numérico genérico reusado con distinto significado según
 * {@code distributionStrategy} (peso, porcentaje o monto fijo) — queda {@code null} cuando
 * {@code distributionStrategy=DEFAULT}. No hay validación cruzada entre ambos campos: el cálculo
 * real y su resolución en tiempo de distribución son responsabilidad de la etapa de ejecución
 * (Pista B), fuera de alcance de este repo.
 * <p>
 * {@code accountTransferRules} (VPR-9702) declara desde/hacia qué cuentas se mueve el balance,
 * con una condición opcional por regla — puede haber varias combinaciones distintas bajo la
 * misma balance strategy.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BalanceStrategyConfig(
        String amountField,
        BalanceSufficiencyStrategy sufficiencyStrategy,
        AmountDistributionStrategy distributionStrategy,
        BigDecimal distributionValue,
        List<AccountTransferRule> accountTransferRules
) {}
