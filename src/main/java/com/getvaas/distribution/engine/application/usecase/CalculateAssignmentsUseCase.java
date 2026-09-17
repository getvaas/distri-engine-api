package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.ComponentOwnerRule;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Paso 7 del pipeline de ejecución (Bloque 4, VPR-9668): reparte el pool ya distribuible entre los
 * owners configurados en Distribution Rules (VPR-9643), aplicando la {@code distributionStrategy}
 * de cada {@code ComponentOwnerRule} y, si está configurado, el chequeo de balance real
 * ({@code sufficiencyStrategy}, vía {@link CheckAccountBalanceSufficiencyUseCase}). No implementa
 * todavía {@code accountTransferRules} ni {@code deductions} — mover la plata calculada hacia una
 * cuenta y aplicar descuentos es una historia futura; acá se decide cuánto le toca a cada owner.
 * <p>
 * Orden de evaluación: primero las reglas con estrategia que se calculan contra el pool total
 * ({@code PERCENTAGE_OF_POOL}, {@code FIXED_AMOUNT}, {@code PROPORTIONAL_WEIGHT}), después las
 * {@code PERCENTAGE_OF_REMAINING} en cascada (en el orden en que están configuradas), y por último
 * la única regla {@code DEFAULT} permitida, que se lleva lo que quede. Cada monto reclamado pasa
 * por el chequeo de balance antes de restarse del pool — si el chequeo lo reduce (o lo anula), lo
 * no asignado queda disponible para el resto de las reglas / el remanente final. Lo que ninguna
 * regla reclama cae al owner por default (el borrower de la distribución) o al override de
 * {@code remainingBalance} si está configurado.
 */
@Component
@RequiredArgsConstructor
public class CalculateAssignmentsUseCase {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final CheckAccountBalanceSufficiencyUseCase checkAccountBalanceSufficiencyUseCase;

    public List<Assignment> execute(Long companyId, PartitionedPoolFunds funds) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var rulesConfig = config.config().rules();
        var rules = rulesConfig != null && rulesConfig.hasComponentOwners() && rulesConfig.componentOwners() != null
                ? rulesConfig.componentOwners()
                : List.<ComponentOwnerRule>of();

        var totalPool = sumAmounts(funds.distributable());
        var totalWeight = rules.stream()
                .filter(rule -> strategyOf(rule) == AmountDistributionStrategy.PROPORTIONAL_WEIGHT)
                .map(rule -> requireDistributionValue(rule))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var assignments = new ArrayList<Assignment>();
        var remaining = totalPool;
        var remainingStrategyRules = new ArrayList<ComponentOwnerRule>();
        var defaultRules = new ArrayList<ComponentOwnerRule>();

        for (var rule : rules) {
            var amount = switch (strategyOf(rule)) {
                case PERCENTAGE_OF_POOL -> percentageOf(totalPool, requireDistributionValue(rule));
                case FIXED_AMOUNT -> requireDistributionValue(rule);
                case PROPORTIONAL_WEIGHT -> proportionalShare(totalPool, requireDistributionValue(rule), totalWeight);
                case PERCENTAGE_OF_REMAINING -> {
                    remainingStrategyRules.add(rule);
                    yield null;
                }
                case DEFAULT -> {
                    defaultRules.add(rule);
                    yield null;
                }
            };
            if (amount != null) {
                remaining = addAssignmentIfAny(assignments, rule, applyBalanceCheck(amount, rule), remaining, totalPool);
            }
        }

        for (var rule : remainingStrategyRules) {
            var amount = applyBalanceCheck(percentageOf(remaining, requireDistributionValue(rule)), rule);
            remaining = addAssignmentIfAny(assignments, rule, amount, remaining, totalPool);
        }

        if (defaultRules.size() > 1) {
            throw new AmbiguousDefaultDistributionStrategyException(defaultRules.size());
        }
        if (!defaultRules.isEmpty()) {
            var rule = defaultRules.get(0);
            var amount = applyBalanceCheck(remaining, rule);
            remaining = addAssignmentIfAny(assignments, rule, amount, remaining, totalPool);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            assignments.add(new Assignment(defaultOwner(companyId, rulesConfig), remaining));
        }

        return assignments;
    }

    private BigDecimal addAssignmentIfAny(List<Assignment> assignments, ComponentOwnerRule rule, BigDecimal amount,
                                           BigDecimal remaining, BigDecimal totalPool) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            assignments.add(new Assignment(rule.owner(), amount));
        }
        var newRemaining = remaining.subtract(amount);
        requireNotOverAllocated(newRemaining, totalPool);
        return newRemaining;
    }

    private BigDecimal applyBalanceCheck(BigDecimal amount, ComponentOwnerRule rule) {
        if (rule.balanceStrategy() == null) {
            return amount;
        }
        return checkAccountBalanceSufficiencyUseCase.execute(amount, rule.balanceStrategy());
    }

    private AmountDistributionStrategy strategyOf(ComponentOwnerRule rule) {
        if (rule.balanceStrategy() == null || rule.balanceStrategy().distributionStrategy() == null) {
            return AmountDistributionStrategy.DEFAULT;
        }
        return rule.balanceStrategy().distributionStrategy();
    }

    private BigDecimal requireDistributionValue(ComponentOwnerRule rule) {
        var value = rule.balanceStrategy() != null ? rule.balanceStrategy().distributionValue() : null;
        if (value == null) {
            throw new InvalidDistributionConfigException(
                    "La regla de '" + rule.owner() + "' (" + strategyOf(rule)
                            + ") requiere 'distributionValue' configurado");
        }
        return value;
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal proportionalShare(BigDecimal totalPool, BigDecimal weight, BigDecimal totalWeight) {
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return totalPool.multiply(weight).divide(totalWeight, SCALE, RoundingMode.HALF_UP);
    }

    private String defaultOwner(Long companyId, DistributionRulesConfig rulesConfig) {
        var remainingBalance = rulesConfig != null ? rulesConfig.remainingBalance() : null;
        if (remainingBalance != null && remainingBalance.destinationAccountId() != null) {
            return String.valueOf(remainingBalance.destinationAccountId());
        }
        return String.valueOf(companyId);
    }

    private BigDecimal sumAmounts(List<PoolFund> funds) {
        return funds.stream().map(PoolFund::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireNotOverAllocated(BigDecimal remaining, BigDecimal totalPool) {
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new AssignmentAllocationExceedsPoolException(totalPool.subtract(remaining), totalPool);
        }
    }
}
