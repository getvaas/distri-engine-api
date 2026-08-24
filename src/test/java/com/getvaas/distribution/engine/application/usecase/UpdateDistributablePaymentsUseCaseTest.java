package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributablePaymentsConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;
import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountingPaymentsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DateTimeFilterRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateGatewayFiltersRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePaymentFiltersRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDistributablePaymentsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateDistributablePaymentsUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null);
    private static final AccountingPaymentsRequest NO_ACCOUNTING_PAYMENTS =
            new AccountingPaymentsRequest(false, null, null);

    private DistributionConfig existingDomain(DistributionConfigPayload payload) {
        return new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private void mockExisting(DistributionConfigPayload payload) {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain(payload));
        when(repository.save(entity)).thenReturn(entity);
    }

    private PaymentFilterConditionGroupRequest group(PaymentFilterConditionRequest... conditions) {
        return new PaymentFilterConditionGroupRequest(List.of(conditions));
    }

    private DistributablePaymentsConfig captureSavedDistributablePayments() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue().distributablePayments();
    }

    // ===== Accounting Payments (VPR-9631) =====

    @Test
    void execute_hasAccountingPaymentsWithValidGroups_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, false,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, "accounting")))),
                null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.accountingPayments().hasAccountingPayments()).isTrue();
        assertThat(saved.accountingPayments().distributeAccountingPayments()).isFalse();
        assertThat(saved.accountingPayments().conditionGroups()).hasSize(1);
        var condition = saved.accountingPayments().conditionGroups().get(0).conditions().get(0);
        assertThat(condition.field()).isEqualTo("payment_type");
        assertThat(condition.operator()).isEqualTo(PaymentFilterOperator.EQ);
        assertThat(condition.value()).isEqualTo("accounting");
    }

    @Test
    void execute_hasAccountingPaymentsWithoutGroups_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(
                new AccountingPaymentsRequest(true, true, List.of()), null, null, null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_groupWithoutConditions_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true, List.of(new PaymentFilterConditionGroupRequest(List.of()))), null, null, null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_conditionMissingFieldOrOperator_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest(null, PaymentFilterOperator.EQ, "x")))), null, null, null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_isNullOperatorWithoutValue_isValid() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest("owner", PaymentFilterOperator.IS_NULL, null)))), null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.accountingPayments().conditionGroups().get(0).conditions().get(0).value()).isNull();
    }

    @Test
    void execute_nonNullableOperatorWithoutValue_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                true, true,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, null)))), null, null, null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_hasAccountingPaymentsFalse_forcesDistributeTrueAndEmptyGroups() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(new AccountingPaymentsRequest(
                false, false,
                List.of(group(new PaymentFilterConditionRequest("payment_type", PaymentFilterOperator.EQ, "accounting")))), null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.accountingPayments().hasAccountingPayments()).isFalse();
        assertThat(saved.accountingPayments().distributeAccountingPayments()).isTrue();
        assertThat(saved.accountingPayments().conditionGroups()).isEmpty();
    }

    @Test
    void execute_preservesRestOfPayload() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null);
        mockExisting(existingPayload);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        useCase.execute("id-1", request);

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }

    // ===== Gateway Filters (VPR-9632) =====

    @Test
    void execute_gatewayFiltersIncludeOnlyWithGateways_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.INCLUDE_ONLY, List.of("PayU", "WOMPI")), null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.INCLUDE_ONLY);
        assertThat(saved.gatewayFilters().gateways()).containsExactly("PayU", "WOMPI");
    }

    @Test
    void execute_gatewayFiltersExcludeWithGateways_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.EXCLUDE, List.of("EFECTY")), null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.EXCLUDE);
        assertThat(saved.gatewayFilters().gateways()).containsExactly("EFECTY");
    }

    @Test
    void execute_gatewayFiltersAllModeWithGatewaysSent_ignoresGatewaysAndPersistsEmptyList() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.ALL, List.of("PayU")), null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.ALL);
        assertThat(saved.gatewayFilters().gateways()).isEmpty();
    }

    @Test
    void execute_gatewayFiltersIncludeOnlyWithoutGateways_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS,
                new UpdateGatewayFiltersRequest(GatewayFilterMode.INCLUDE_ONLY, List.of()), null, null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_gatewayFiltersModeNotSent_defaultsToAll() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.gatewayFilters().mode()).isEqualTo(GatewayFilterMode.ALL);
        assertThat(saved.gatewayFilters().gateways()).isEmpty();
    }

    // ===== Conciliation Requirements (VPR-9633) =====

    @Test
    void execute_conciliationRequirementsWithValidRule_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, "PayU")));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.conciliationRequirements().groups()).hasSize(1);
        var rule = saved.conciliationRequirements().groups().get(0).rules().get(0);
        assertThat(rule.tableA()).isEqualTo(ConciliationTable.PAYMENT_TAPE);
        assertThat(rule.tableB()).isEqualTo(ConciliationTable.PAYMENTS);
        assertThat(rule.gateway()).isEqualTo("PayU");
    }

    @Test
    void execute_conciliationRequirementsRuleWithNullGateway_persistsAsAllGateways() {
        mockExisting(EMPTY_PAYLOAD);
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENTS, ConciliationTable.BORROWER_CORE, null)));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.conciliationRequirements().groups().get(0).rules().get(0).gateway()).isNull();
    }

    @Test
    void execute_conciliationRequirementsRuleWithSameTable_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var group = new ConciliationRequirementGroupRequest(List.of(
                new ConciliationRequirementRuleRequest(ConciliationTable.PAYMENTS, ConciliationTable.PAYMENTS, null)));
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, List.of(group), null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_conciliationRequirementsGroupWithoutRules_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null,
                List.of(new ConciliationRequirementGroupRequest(List.of())), null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_conciliationRequirementsNotSent_persistsEmptyListWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.conciliationRequirements().groups()).isEmpty();
    }

    // ===== Date & Time Filters (VPR-9634) =====

    @Test
    void execute_dateTimeFilterDistributeByDateWithOperatorAndValue_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE,
                DateTimeFilterOperator.IS_BEFORE, "today", null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        var savedRule = saved.dateTimeFilters().rules().get(0);
        assertThat(savedRule.gateway()).isNull();
        assertThat(savedRule.ruleType()).isEqualTo(DateTimeFilterRuleType.DISTRIBUTE_BY_DATE);
        assertThat(savedRule.operator()).isEqualTo(DateTimeFilterOperator.IS_BEFORE);
        assertThat(savedRule.value()).isEqualTo("today");
        assertThat(savedRule.maxDays()).isNull();
    }

    @Test
    void execute_dateTimeFilterDaysBackLimitWithMaxDays_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 3);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        var savedRule = saved.dateTimeFilters().rules().get(0);
        assertThat(savedRule.gateway()).isEqualTo("EFECTY");
        assertThat(savedRule.ruleType()).isEqualTo(DateTimeFilterRuleType.DAYS_BACK_LIMIT);
        assertThat(savedRule.maxDays()).isEqualTo(3);
        assertThat(savedRule.operator()).isNull();
        assertThat(savedRule.value()).isNull();
    }

    @Test
    void execute_dateTimeFilterDistributeByDateWithoutOperator_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE_TIME, null, "today", null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_dateTimeFilterDistributeByDateWithoutValue_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE, DateTimeFilterOperator.IS_AFTER, null, null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_dateTimeFilterDaysBackLimitWithoutMaxDays_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, null);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_dateTimeFilterDaysBackLimitWithNonPositiveMaxDays_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var rule = new DateTimeFilterRuleRequest("EFECTY", DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 0);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, List.of(rule));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_dateTimeFiltersNotSent_persistsEmptyListWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdatePaymentFiltersRequest(NO_ACCOUNTING_PAYMENTS, null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedDistributablePayments();
        assertThat(saved.dateTimeFilters().rules()).isEmpty();
    }
}
