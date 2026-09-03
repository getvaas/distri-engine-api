package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;
import com.getvaas.distribution.engine.domain.model.enums.DeductionPeriodicity;
import com.getvaas.distribution.engine.domain.model.enums.DeductionType;
import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;
import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountTransferRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.BalanceStrategyConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DeductionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.RemainingBalanceConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistributionRulesConfigBuilderTest {

    private final DistributionRulesConfigBuilder builder = new DistributionRulesConfigBuilder();

    @Test
    void build_rulesForAll4Components_persistsAsIs() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.INTEREST, "funder", null, null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.LATE_FEE, "servicer", "late fees go to servicer", null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.GUARANTEE, "guarantee_fund", null, null, null)), null);

        DistributionRulesConfig saved = builder.build(request);

        assertThat(saved.componentOwners()).hasSize(4);
        assertThat(saved.componentOwners().get(2).owner()).isEqualTo("servicer");
        assertThat(saved.componentOwners().get(2).description()).isEqualTo("late fees go to servicer");
    }

    @Test
    void build_ruleWithoutComponent_throwsInvalidDistributionConfigException() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(null, "funder", null, null, null)), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_ruleWithoutOwner_throwsInvalidDistributionConfigException() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, null, null, null, null)), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_duplicateComponent_throwsInvalidDistributionConfigException() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "servicer", null, null, null)), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_emptyOrMissingRules_persistsEmptyListWithoutError() {
        var request = new UpdateDistributionRulesRequest(false, null, null);

        DistributionRulesConfig saved = builder.build(request);

        assertThat(saved.componentOwners()).isEmpty();
    }

    @Test
    void build_ruleWithBalanceStrategy_persists() {
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.PERCENTAGE_OF_POOL,
                new BigDecimal("25.5"), null);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy();

        assertThat(saved.amountField()).isEqualTo("net_amount");
        assertThat(saved.sufficiencyStrategy()).isEqualTo(BalanceSufficiencyStrategy.UNTIL_EXHAUSTED);
        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.PERCENTAGE_OF_POOL);
        assertThat(saved.distributionValue()).isEqualByComparingTo("25.5");
    }

    @Test
    void build_ruleWithoutBalanceStrategy_persistsAsNull() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy();

        assertThat(saved).isNull();
    }

    @Test
    void build_fixedAmountWithoutDistributionValue_persistsWithoutError() {
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.IGNORE_BALANCE, AmountDistributionStrategy.FIXED_AMOUNT, null, null);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy();

        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.FIXED_AMOUNT);
        assertThat(saved.distributionValue()).isNull();
    }

    @Test
    void build_defaultWithDistributionValueSet_persistsAsIs() {
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.SUFFICIENT_OR_STOP, AmountDistributionStrategy.DEFAULT,
                new BigDecimal("100"), null);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy();

        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.DEFAULT);
        assertThat(saved.distributionValue()).isEqualByComparingTo("100");
    }

    @Test
    void build_hasComponentOwnersTrue_persists() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null)), null);

        assertThat(builder.build(request).hasComponentOwners()).isTrue();
    }

    @Test
    void build_hasComponentOwnersFalseWithEmptyList_persists() {
        var request = new UpdateDistributionRulesRequest(false, List.of(), null);

        DistributionRulesConfig saved = builder.build(request);

        assertThat(saved.hasComponentOwners()).isFalse();
        assertThat(saved.componentOwners()).isEmpty();
    }

    @Test
    void build_hasComponentOwnersFalseWithData_persistsWithoutError() {
        var request = new UpdateDistributionRulesRequest(false, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null)), null);

        DistributionRulesConfig saved = builder.build(request);

        assertThat(saved.hasComponentOwners()).isFalse();
        assertThat(saved.componentOwners()).hasSize(1);
    }

    @Test
    void build_hasComponentOwnersNotSent_defaultsToFalse() {
        var request = new UpdateDistributionRulesRequest(null, null, null);

        assertThat(builder.build(request).hasComponentOwners()).isFalse();
    }

    @Test
    void build_accountTransferRuleWithCondition_persists() {
        var condition = new PaymentFilterConditionRequest("contract_id", PaymentFilterOperator.EQ, "123");
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L, 2L), List.of(3L), condition, null);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy().accountTransferRules().get(0);

        assertThat(saved.fromAccountIds()).containsExactly(1L, 2L);
        assertThat(saved.toAccountIds()).containsExactly(3L);
        assertThat(saved.condition().field()).isEqualTo("contract_id");
        assertThat(saved.condition().operator()).isEqualTo(PaymentFilterOperator.EQ);
        assertThat(saved.condition().value()).isEqualTo("123");
    }

    @Test
    void build_accountTransferRuleWithoutCondition_persistsAsNull() {
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(2L), null, null);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy().accountTransferRules().get(0);

        assertThat(saved.condition()).isNull();
    }

    @Test
    void build_emptyOrMissingAccountTransferRules_persistsEmptyList() {
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT, null, null);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy();

        assertThat(saved.accountTransferRules()).isEmpty();
    }

    @Test
    void build_sameAccountIdInFromAndTo_persistsWithoutError() {
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(1L), null, null);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy().accountTransferRules().get(0);

        assertThat(saved.fromAccountIds()).containsExactly(1L);
        assertThat(saved.toAccountIds()).containsExactly(1L);
    }

    @Test
    void build_deductionWithAllFields_persists() {
        var deduction = new DeductionRequest("servicing_fee", DeductionType.PERCENTAGE,
                new BigDecimal("2.5"), 9L, DeductionPeriodicity.ONCE_PER_DISTRIBUTION);
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(2L), null, List.of(deduction));
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy()
                .accountTransferRules().get(0).deductions().get(0);

        assertThat(saved.concept()).isEqualTo("servicing_fee");
        assertThat(saved.type()).isEqualTo(DeductionType.PERCENTAGE);
        assertThat(saved.value()).isEqualByComparingTo("2.5");
        assertThat(saved.accountId()).isEqualTo(9L);
        assertThat(saved.periodicity()).isEqualTo(DeductionPeriodicity.ONCE_PER_DISTRIBUTION);
    }

    @Test
    void build_deductionWithNullAccountId_persistsAsIs() {
        var deduction = new DeductionRequest("origination_fee", DeductionType.FIXED,
                new BigDecimal("50"), null, DeductionPeriodicity.ALWAYS);
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(2L), null, List.of(deduction));
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy()
                .accountTransferRules().get(0).deductions().get(0);

        assertThat(saved.accountId()).isNull();
    }

    @Test
    void build_emptyOrMissingDeductions_persistsEmptyList() {
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(2L), null, null);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy().accountTransferRules().get(0);

        assertThat(saved.deductions()).isEmpty();
    }

    @Test
    void build_multipleDeductions_persistsAll() {
        var deduction1 = new DeductionRequest("servicing_fee", DeductionType.FIXED,
                new BigDecimal("10"), null, DeductionPeriodicity.ONCE_PER_MONTH);
        var deduction2 = new DeductionRequest("origination_fee", DeductionType.PERCENTAGE,
                new BigDecimal("1.5"), 9L, DeductionPeriodicity.ONCE_PER_WEEK);
        var accountTransferRule = new AccountTransferRuleRequest(List.of(1L), List.of(2L), null,
                List.of(deduction1, deduction2));
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.DEFAULT,
                null, List.of(accountTransferRule));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy, null)), null);

        var saved = builder.build(request).componentOwners().get(0).balanceStrategy().accountTransferRules().get(0);

        assertThat(saved.deductions()).hasSize(2);
        assertThat(saved.deductions().get(1).concept()).isEqualTo("origination_fee");
    }

    @Test
    void build_remainingBalanceWithAllFields_persists() {
        var remainingBalance = new RemainingBalanceConfigRequest(PaymentComponent.LATE_FEE, 42L);
        var request = new UpdateDistributionRulesRequest(false, null, remainingBalance);

        var saved = builder.build(request).remainingBalance();

        assertThat(saved.component()).isEqualTo(PaymentComponent.LATE_FEE);
        assertThat(saved.destinationAccountId()).isEqualTo(42L);
    }

    @Test
    void build_remainingBalanceNotSent_persistsAsNull() {
        var request = new UpdateDistributionRulesRequest(false, null, null);

        assertThat(builder.build(request).remainingBalance()).isNull();
    }

    @Test
    void build_remainingBalanceWithOnlyOneField_persistsWithoutError() {
        var remainingBalance = new RemainingBalanceConfigRequest(PaymentComponent.PRINCIPAL, null);
        var request = new UpdateDistributionRulesRequest(false, null, remainingBalance);

        var saved = builder.build(request).remainingBalance();

        assertThat(saved.component()).isEqualTo(PaymentComponent.PRINCIPAL);
        assertThat(saved.destinationAccountId()).isNull();
    }

    @Test
    void build_distributeAccountingPaymentsTrue_persists() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, true)), null);

        assertThat(builder.build(request).componentOwners().get(0).distributeAccountingPayments()).isTrue();
    }

    @Test
    void build_distributeAccountingPaymentsFalse_persists() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, false)), null);

        assertThat(builder.build(request).componentOwners().get(0).distributeAccountingPayments()).isFalse();
    }

    @Test
    void build_distributeAccountingPaymentsNotSent_defaultsToFalse() {
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null, null)), null);

        assertThat(builder.build(request).componentOwners().get(0).distributeAccountingPayments()).isFalse();
    }
}
