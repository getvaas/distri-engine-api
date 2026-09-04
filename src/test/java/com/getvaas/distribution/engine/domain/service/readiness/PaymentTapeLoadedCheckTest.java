package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTapeLoadedCheckTest {

    @Mock
    private WorkingDaysCalculator workingDaysCalculator;
    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;
    @InjectMocks
    private PaymentTapeLoadedCheck check;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    private ReadinessCheckContext contextWithPool(PoolConfig pool) {
        return new ReadinessCheckContext(3L, DATE, "Colombia (COL)", 3L, pool);
    }

    @Test
    void evaluate_paymentTapeStrategyWithCandidates_passes() {
        var pool = new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig("net_amount", 30), null, null);
        when(workingDaysCalculator.subtractWorkingDays(eq(DATE), eq(30), anyString())).thenReturn(DATE.minusDays(30));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(any(), any(), any()))
                .thenReturn(List.of(PaymentTapeEntity.builder().id("pt-1").companyId(3L).build()));

        var result = check.evaluate(contextWithPool(pool));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_paymentTapeStrategyWithoutCandidates_fails() {
        var pool = new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig("net_amount", 30), null, null);
        when(workingDaysCalculator.subtractWorkingDays(eq(DATE), eq(30), anyString())).thenReturn(DATE.minusDays(30));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(any(), any(), any()))
                .thenReturn(List.of());

        var result = check.evaluate(contextWithPool(pool));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
        assertThat(result.reason()).contains("3");
    }

    @Test
    void evaluate_paymentTapeStrategyWithoutDaysBack_defaultsTo90() {
        var pool = new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig("net_amount", null), null, null);
        when(workingDaysCalculator.subtractWorkingDays(eq(DATE), eq(90), anyString())).thenReturn(DATE.minusDays(90));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(any(), any(), any()))
                .thenReturn(List.of(PaymentTapeEntity.builder().id("pt-1").companyId(3L).build()));

        var result = check.evaluate(contextWithPool(pool));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verify(workingDaysCalculator).subtractWorkingDays(DATE, 90, "Colombia (COL)");
    }

    @Test
    void evaluate_accountBalanceStrategy_passesWithoutQueryingRepository() {
        var pool = new PoolConfig(PoolStrategyType.ACCOUNT_BALANCE, null, null, null);

        var result = check.evaluate(contextWithPool(pool));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_dataSourceAggregationStrategy_passesWithoutQueryingRepository() {
        var pool = new PoolConfig(PoolStrategyType.DATA_SOURCE_AGGREGATION, null, null, null);

        var result = check.evaluate(contextWithPool(pool));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_nullPool_passesWithoutQueryingRepository() {
        var result = check.evaluate(contextWithPool(null));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }
}
