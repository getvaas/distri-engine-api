package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PaymentFiltersConfig;
import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;
import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountingPaymentsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DateTimeFilterRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateGatewayFiltersRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePaymentFiltersRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentFiltersConfigBuilderTest {

    private final PaymentFiltersConfigBuilder builder = new PaymentFiltersConfigBuilder();

    private static final AccountingPaymentsRequest NO_ACCOUNTING_PAYMENTS =
            new AccountingPaymentsRequest(false, null, null);

    private PaymentFilterConditionGroupRequest group(PaymentFilterConditionRequest... conditions) {
        return new PaymentFilterConditionGroupRequest(List.of(conditions));
    }

    // ===== Accounting Payments (VPR-9631) =====

    @Test
    void build_hasAccountingPaymentsWithValidGroups_persistsAsIs() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, false,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, "accounting")))),
                null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.accountingPayments().hasAccountingPayments()).isTrue();
        assertThat(saved.accountingPayments().distributeAccountingPayments()).isFalse();
        assertThat(saved.accountingPayments().conditionGroups()).hasSize(1);
        var condition = saved.accountingPayments().conditionGroups().get(0).conditions().get(0);
        assertThat(condition.field()).isEqualTo("payment_type");
        assertThat(condition.operator()).isEqualTo(PaymentFilterOperator.EQ);
        assertThat(condition.value()).isEqualTo("accounting");
    }

    @Test
    void build_hasAccountingPaymentsWithoutGroups_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(
                new AccountingPaymentsRequest(true, true, List.of()), null, null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_groupWithoutConditions_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true, List.of(new PaymentFilterConditionGroupRequest(List.of()))), null, null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_conditionMissingFieldOrOperator_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest(null, PaymentFilterOperator.EQ, "x")))), null, null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_isNullOperatorWithoutValue_isValid() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest("owner", PaymentFilterOperator.IS_NULL, null)))), null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.accountingPayments().conditionGroups().get(0).conditions().get(0).value()).isNull();
    }

    @Test
    void build_nonNullableOperatorWithoutValue_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, null)))), null, null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_hasAccountingPaymentsFalse_forcesDistributeTrueAndEmptyGroups() {
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                false, false,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, "accounting")))), null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.accountingPayments().hasAccountingPayments()).isFalse();
        assertThat(saved.accountingPayments().distributeAccountingPayments()).isTrue();
        assertThat(saved.accountingPayments().conditionGroups()).isEmpty();
    }

    // ===== Gateway Filters (VPR-9632) =====

    @Test
    void build_gatewayFiltersIncludeOnlyWithGateways_persistsAsIs() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.INCLUDE_ONLY, List.of("PayU", "WOMPI")), null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.INCLUDE_ONLY);
        assertThat(saved.gatewayFilters().gateways()).containsExactly("PayU", "WOMPI");
    }

    @Test
    void build_gatewayFiltersExcludeWithGateways_persistsAsIs() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.EXCLUDE, List.of("EFECTY")), null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.EXCLUDE);
        assertThat(saved.gatewayFilters().gateways()).containsExactly("EFECTY");
    }

    @Test
    void build_gatewayFiltersAllModeWithGatewaysSent_ignoresGatewaysAndPersistsEmptyList() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.ALL, List.of("PayU")), null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.ALL);
        assertThat(saved.gatewayFilters().gateways()).isEmpty();
    }

    @Test
    void build_gatewayFiltersIncludeOnlyWithoutGateways_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.INCLUDE_ONLY, List.of()), null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_gatewayFiltersModeNotSent_defaultsToAll() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.ALL);
        assertThat(saved.gatewayFilters().gateways()).isEmpty();
    }

    // ===== Conciliation Requirements (VPR-9633) =====

    @Test
    void build_conciliationRequirementsWithValidRule_persistsAsIs() {
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, "PayU")));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.conciliationRequirements().groups()).hasSize(1);
        var rule = saved.conciliationRequirements().groups().get(0).rules().get(0);
        assertThat(rule.tableA()).isEqualTo(ConciliationTable.PAYMENT_TAPE);
        assertThat(rule.tableB()).isEqualTo(ConciliationTable.PAYMENTS);
        assertThat(rule.gateway()).isEqualTo("PayU");
    }

    @Test
    void build_conciliationRequirementsRuleWithNullGateway_persistsAsAllGateways() {
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENTS, ConciliationTable.BORROWER_CORE, null)));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.conciliationRequirements().groups().get(0).rules().get(0).gateway()).isNull();
    }

    @Test
    void build_conciliationRequirementsRuleWithSameTable_throwsInvalidDistributionConfigException() {
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENTS, ConciliationTable.PAYMENTS, null)));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_conciliationRequirementsGroupWithoutRules_throwsInvalidDistributionConfigException() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null,
                List.of(new ConciliationRequirementGroupRequest(List.of())), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_conciliationRequirementsNotSent_persistsEmptyListWithoutError() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.conciliationRequirements().groups()).isEmpty();
    }

    // ===== Date & Time Filters (VPR-9634) =====

    @Test
    void build_dateTimeFilterDistributeByDateWithOperatorAndValue_persistsAsIs() {
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE,
                DateTimeFilterOperator.IS_BEFORE, "today", null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        PaymentFiltersConfig saved = builder.build(request);

        var savedRule = saved.dateTimeFilters().rules().get(0);
        assertThat(savedRule.gateway()).isNull();
        assertThat(savedRule.ruleType()).isEqualTo(DateTimeFilterRuleType.DISTRIBUTE_BY_DATE);
        assertThat(savedRule.operator()).isEqualTo(DateTimeFilterOperator.IS_BEFORE);
        assertThat(savedRule.value()).isEqualTo("today");
        assertThat(savedRule.maxDays()).isNull();
    }

    @Test
    void build_dateTimeFilterDaysBackLimitWithMaxDays_persistsAsIs() {
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 3);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        PaymentFiltersConfig saved = builder.build(request);

        var savedRule = saved.dateTimeFilters().rules().get(0);
        assertThat(savedRule.gateway()).isEqualTo("EFECTY");
        assertThat(savedRule.ruleType()).isEqualTo(DateTimeFilterRuleType.DAYS_BACK_LIMIT);
        assertThat(savedRule.maxDays()).isEqualTo(3);
        assertThat(savedRule.operator()).isNull();
        assertThat(savedRule.value()).isNull();
    }

    @Test
    void build_dateTimeFilterDistributeByDateWithoutOperator_throwsInvalidDistributionConfigException() {
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE_TIME, null, "today", null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_dateTimeFilterDistributeByDateWithoutValue_throwsInvalidDistributionConfigException() {
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE, DateTimeFilterOperator.IS_AFTER, null, null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_dateTimeFilterDaysBackLimitWithoutMaxDays_throwsInvalidDistributionConfigException() {
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_dateTimeFilterDaysBackLimitWithNonPositiveMaxDays_throwsInvalidDistributionConfigException() {
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 0);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_dateTimeFiltersNotSent_persistsEmptyListWithoutError() {
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        PaymentFiltersConfig saved = builder.build(request);

        assertThat(saved.dateTimeFilters().rules()).isEmpty();
    }
}
