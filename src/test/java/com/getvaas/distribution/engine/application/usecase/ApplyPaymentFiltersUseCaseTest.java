package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountingPaymentsConfig;
import com.getvaas.distribution.engine.domain.model.DateTimeFilterRule;
import com.getvaas.distribution.engine.domain.model.DateTimeFiltersConfig;
import com.getvaas.distribution.engine.domain.model.GatewayFiltersConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFilterCondition;
import com.getvaas.distribution.engine.domain.model.PaymentFilterConditionGroup;
import com.getvaas.distribution.engine.domain.model.PaymentFiltersConfig;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;
import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplyPaymentFiltersUseCaseTest {

    private ApplyPaymentFiltersUseCase useCase;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    @BeforeEach
    void setUp() {
        useCase = new ApplyPaymentFiltersUseCase(new WorkingDaysCalculator());
    }

    private PaymentTapeEntity tape(String id, LocalDateTime paymentDate, String gatewayCode, BigDecimal netAmount) {
        return PaymentTapeEntity.builder().id(id).companyId(3L).paymentDate(paymentDate)
                .gatewayCode(gatewayCode).netAmount(netAmount).build();
    }

    private PaymentFiltersConfig configWith(AccountingPaymentsConfig accountingPayments, GatewayFiltersConfig gatewayFilters,
            DateTimeFiltersConfig dateTimeFilters) {
        return new PaymentFiltersConfig(accountingPayments, gatewayFilters, null, dateTimeFilters);
    }

    @Test
    void execute_nullPaymentFilters_returnsTapesUnchanged() {
        var tapes = List.of(tape("pt-1", null, null, null));

        var result = useCase.execute(null, tapes, DATE, "Colombia (COL)");

        assertThat(result).isEqualTo(tapes);
    }

    // ===== Accounting Payments =====

    @Test
    void execute_accountingPaymentsDisabled_keepsAllTapes() {
        var config = configWith(new AccountingPaymentsConfig(false, true, List.of()), null, null);
        var tapes = List.of(tape("pt-1", null, null, new BigDecimal("10")));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).hasSize(1);
    }

    @Test
    void execute_accountingPaymentsMatchedButDistributeTrue_keepsTape() {
        var group = new PaymentFilterConditionGroup(List.of(new PaymentFilterCondition("net_amount", PaymentFilterOperator.GT, "5")));
        var config = configWith(new AccountingPaymentsConfig(true, true, List.of(group)), null, null);
        var tapes = List.of(tape("pt-1", null, null, new BigDecimal("10")));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).hasSize(1);
    }

    @Test
    void execute_accountingPaymentsMatchedAndDistributeFalse_excludesTape() {
        var group = new PaymentFilterConditionGroup(List.of(new PaymentFilterCondition("net_amount", PaymentFilterOperator.GT, "5")));
        var config = configWith(new AccountingPaymentsConfig(true, false, List.of(group)), null, null);
        var tapes = List.of(
                tape("pt-1", null, null, new BigDecimal("10")),
                tape("pt-2", null, null, new BigDecimal("1")));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-2");
    }

    @Test
    void execute_unsupportedField_throws() {
        var group = new PaymentFilterConditionGroup(List.of(new PaymentFilterCondition("unknown_field", PaymentFilterOperator.EQ, "x")));
        var config = configWith(new AccountingPaymentsConfig(true, false, List.of(group)), null, null);
        var tapes = List.of(tape("pt-1", null, null, null));

        assertThatThrownBy(() -> useCase.execute(config, tapes, DATE, "Colombia (COL)"))
                .isInstanceOf(UnsupportedPaymentFilterFieldException.class);
    }

    @Test
    void execute_gtOperatorOnNonNumericField_throws() {
        var group = new PaymentFilterConditionGroup(List.of(new PaymentFilterCondition("id", PaymentFilterOperator.GT, "5")));
        var config = configWith(new AccountingPaymentsConfig(true, false, List.of(group)), null, null);
        var tapes = List.of(tape("pt-1", null, null, null));

        assertThatThrownBy(() -> useCase.execute(config, tapes, DATE, "Colombia (COL)"))
                .isInstanceOf(UnsupportedPaymentFilterFieldException.class);
    }

    // ===== Gateway Filters =====

    @Test
    void execute_gatewayModeAll_keepsAllTapes() {
        var config = configWith(null, new GatewayFiltersConfig(GatewayFilterMode.ALL, List.of()), null);
        var tapes = List.of(tape("pt-1", null, "PayU", null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).hasSize(1);
    }

    @Test
    void execute_gatewayIncludeOnly_keepsOnlyMatchingGateway() {
        var config = configWith(null, new GatewayFiltersConfig(GatewayFilterMode.INCLUDE_ONLY, List.of("PayU")), null);
        var tapes = List.of(tape("pt-1", null, "PayU", null), tape("pt-2", null, "EFECTY", null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-1");
    }

    @Test
    void execute_gatewayExclude_removesMatchingGateway() {
        var config = configWith(null, new GatewayFiltersConfig(GatewayFilterMode.EXCLUDE, List.of("EFECTY")), null);
        var tapes = List.of(tape("pt-1", null, "PayU", null), tape("pt-2", null, "EFECTY", null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-1");
    }

    // ===== Date & Time Filters =====

    @Test
    void execute_distributeByDateIsBeforeToday_keepsOnlyPastTapes() {
        var rule = new DateTimeFilterRule(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE,
                DateTimeFilterOperator.IS_BEFORE, "today", null, null, null);
        var config = configWith(null, null, new DateTimeFiltersConfig(List.of(rule)));
        var tapes = List.of(
                tape("pt-past", DATE.minusDays(1).atStartOfDay(), null, null),
                tape("pt-today", DATE.atStartOfDay(), null, null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-past");
    }

    @Test
    void execute_distributeByDateTimeWithCutoffTime_comparesFullDateTime() {
        var rule = new DateTimeFilterRule(null, DateTimeFilterRuleType.DISTRIBUTE_BY_DATE_TIME,
                DateTimeFilterOperator.IS_BEFORE, "today", null, LocalTime.of(17, 0), null);
        var config = configWith(null, null, new DateTimeFiltersConfig(List.of(rule)));
        var tapes = List.of(
                tape("pt-before-cutoff", LocalDateTime.of(DATE, LocalTime.of(10, 0)), null, null),
                tape("pt-after-cutoff", LocalDateTime.of(DATE, LocalTime.of(18, 0)), null, null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-before-cutoff");
    }

    @Test
    void execute_daysBackLimitCalendarDays_excludesOlderTapes() {
        var rule = new DateTimeFilterRule(null, DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 5, null, false);
        var config = configWith(null, null, new DateTimeFiltersConfig(List.of(rule)));
        var tapes = List.of(
                tape("pt-within", DATE.minusDays(3).atStartOfDay(), null, null),
                tape("pt-outside", DATE.minusDays(10).atStartOfDay(), null, null));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-within");
    }

    @Test
    void execute_ruleWithSpecificGateway_onlyAppliesToMatchingTapes() {
        // regla exige DISTRIBUTE_BY_DATE IS_BEFORE today, pero solo para EFECTY
        var rule = new DateTimeFilterRule("EFECTY", DateTimeFilterRuleType.DISTRIBUTE_BY_DATE,
                DateTimeFilterOperator.IS_BEFORE, "today", null, null, null);
        var config = configWith(null, null, new DateTimeFiltersConfig(List.of(rule)));
        var tapes = List.of(
                tape("pt-efecty-today", DATE.atStartOfDay(), "EFECTY", null), // no cumple, gateway coincide -> excluida
                tape("pt-payu-today", DATE.atStartOfDay(), "PayU", null)); // gateway no coincide -> regla no aplica, se mantiene

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-payu-today");
    }

    @Test
    void execute_allThreeFilters_applyInSequence() {
        var accountingGroup = new PaymentFilterConditionGroup(List.of(new PaymentFilterCondition("net_amount", PaymentFilterOperator.LT, "0")));
        var config = new PaymentFiltersConfig(
                new AccountingPaymentsConfig(true, false, List.of(accountingGroup)),
                new GatewayFiltersConfig(GatewayFilterMode.EXCLUDE, List.of("EFECTY")),
                null,
                new DateTimeFiltersConfig(List.of(new DateTimeFilterRule(null, DateTimeFilterRuleType.DAYS_BACK_LIMIT, null, null, 5, null, false))));
        var tapes = List.of(
                tape("pt-survives", DATE.minusDays(1).atStartOfDay(), "PayU", new BigDecimal("10")),
                tape("pt-wrong-gateway", DATE.minusDays(1).atStartOfDay(), "EFECTY", new BigDecimal("10")),
                tape("pt-too-old", DATE.minusDays(10).atStartOfDay(), "PayU", new BigDecimal("10")));

        var result = useCase.execute(config, tapes, DATE, "Colombia (COL)");

        assertThat(result).extracting(PaymentTapeEntity::getId).containsExactly("pt-survives");
    }
}
