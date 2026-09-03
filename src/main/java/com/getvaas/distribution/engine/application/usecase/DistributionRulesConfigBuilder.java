package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountTransferRule;
import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.domain.model.ComponentOwnerRule;
import com.getvaas.distribution.engine.domain.model.Deduction;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFilterCondition;
import com.getvaas.distribution.engine.domain.model.RemainingBalanceConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountTransferRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.BalanceStrategyConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DeductionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.RemainingBalanceConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Construye la etapa Distribution Rules — owner por componente de la cuota (VPR-9643), primera
 * iteración mínima configurable. Fees/deducciones, multi-moneda por regla, remanente/cascada e
 * impuestos/seguros quedan explícitamente fuera de alcance.
 */
@Component
public class DistributionRulesConfigBuilder {

    public DistributionRulesConfig build(UpdateDistributionRulesRequest request) {
        var enabled = Boolean.TRUE.equals(request.hasComponentOwners());
        var remainingBalance = buildRemainingBalanceConfig(request.remainingBalance());

        var ruleRequests = request.componentOwners();
        if (ruleRequests == null || ruleRequests.isEmpty()) {
            return new DistributionRulesConfig(enabled, List.of(), remainingBalance);
        }

        var seenComponents = new HashSet<>();
        var componentOwners = ruleRequests.stream()
                .map(r -> buildComponentOwnerRule(r, seenComponents))
                .toList();

        return new DistributionRulesConfig(enabled, componentOwners, remainingBalance);
    }

    private RemainingBalanceConfig buildRemainingBalanceConfig(RemainingBalanceConfigRequest request) {
        if (request == null) {
            return null;
        }
        return new RemainingBalanceConfig(request.component(), request.destinationAccountId());
    }

    private ComponentOwnerRule buildComponentOwnerRule(ComponentOwnerRuleRequest ruleRequest, HashSet<Object> seenComponents) {
        if (ruleRequest.component() == null) {
            throw new InvalidDistributionConfigException("cada regla requiere 'component'");
        }
        if (ruleRequest.owner() == null || ruleRequest.owner().isBlank()) {
            throw new InvalidDistributionConfigException("cada regla requiere 'owner'");
        }
        if (!seenComponents.add(ruleRequest.component())) {
            throw new InvalidDistributionConfigException(
                    "el componente " + ruleRequest.component() + " está repetido en 'componentOwners'");
        }

        return new ComponentOwnerRule(ruleRequest.component(), ruleRequest.owner(), ruleRequest.description(),
                buildBalanceStrategyConfig(ruleRequest.balanceStrategy()),
                Boolean.TRUE.equals(ruleRequest.distributeAccountingPayments()));
    }

    private BalanceStrategyConfig buildBalanceStrategyConfig(BalanceStrategyConfigRequest request) {
        if (request == null) {
            return null;
        }
        return new BalanceStrategyConfig(request.amountField(), request.sufficiencyStrategy(),
                request.distributionStrategy(), request.distributionValue(),
                buildAccountTransferRules(request.accountTransferRules()));
    }

    private List<AccountTransferRule> buildAccountTransferRules(List<AccountTransferRuleRequest> ruleRequests) {
        if (ruleRequests == null || ruleRequests.isEmpty()) {
            return List.of();
        }
        return ruleRequests.stream()
                .map(r -> new AccountTransferRule(r.fromAccountIds(), r.toAccountIds(), buildCondition(r.condition()),
                        buildDeductions(r.deductions())))
                .toList();
    }

    private PaymentFilterCondition buildCondition(PaymentFilterConditionRequest request) {
        if (request == null) {
            return null;
        }
        return new PaymentFilterCondition(request.field(), request.operator(), request.value());
    }

    private List<Deduction> buildDeductions(List<DeductionRequest> deductionRequests) {
        if (deductionRequests == null || deductionRequests.isEmpty()) {
            return List.of();
        }
        return deductionRequests.stream()
                .map(d -> new Deduction(d.concept(), d.type(), d.value(), d.accountId(), d.periodicity()))
                .toList();
    }
}
