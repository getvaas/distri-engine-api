package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountTransferRule;
import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.domain.model.ComponentOwnerRule;
import com.getvaas.distribution.engine.domain.model.Deduction;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFilterCondition;
import com.getvaas.distribution.engine.domain.model.RemainingBalanceConfig;
import com.getvaas.distribution.engine.domain.model.enums.PaymentType;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountTransferRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.BalanceStrategyConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DeductionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.RemainingBalanceConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Construye la etapa Distribution Rules — cascada de reglas por owner (VPR-9643, VPR-9698). La
 * lista de reglas es de largo arbitrario, sin identificador de componente ni tope de 4 (ver
 * {@code ComponentOwnerRule}). Fees/deducciones, multi-moneda por regla e impuestos/seguros
 * quedan explícitamente fuera de alcance.
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

        var componentOwners = ruleRequests.stream()
                .map(this::buildComponentOwnerRule)
                .toList();

        return new DistributionRulesConfig(enabled, componentOwners, remainingBalance);
    }

    private RemainingBalanceConfig buildRemainingBalanceConfig(RemainingBalanceConfigRequest request) {
        if (request == null) {
            return null;
        }
        return new RemainingBalanceConfig(request.destinationAccountId(), request.fromAccountId());
    }

    private ComponentOwnerRule buildComponentOwnerRule(ComponentOwnerRuleRequest ruleRequest) {
        if (ruleRequest.owner() == null || ruleRequest.owner().isBlank()) {
            throw new InvalidDistributionConfigException("cada regla requiere 'owner'");
        }

        var paymentTypes = ruleRequest.paymentTypes() == null ? List.<PaymentType>of() : ruleRequest.paymentTypes();

        return new ComponentOwnerRule(ruleRequest.owner(), ruleRequest.description(),
                buildBalanceStrategyConfig(ruleRequest.balanceStrategy()),
                Boolean.TRUE.equals(ruleRequest.distributeAccountingPayments()), ruleRequest.toAccountId(),
                ruleRequest.fromAccountId(), paymentTypes);
    }

    private BalanceStrategyConfig buildBalanceStrategyConfig(BalanceStrategyConfigRequest request) {
        if (request == null) {
            return null;
        }
        return new BalanceStrategyConfig(request.amountField(), request.sufficiencyStrategy(),
                request.accountIdsToCheck(), request.distributionStrategy(), request.distributionValue(),
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
