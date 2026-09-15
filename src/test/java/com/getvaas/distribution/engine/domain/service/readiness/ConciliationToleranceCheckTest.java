package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.application.usecase.UnsupportedConciliationRuleException;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementGroup;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementRule;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementsConfig;
import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliationToleranceCheckTest {

    @Mock
    private WorkingDaysCalculator workingDaysCalculator;
    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;
    @InjectMocks
    private ConciliationToleranceCheck check;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);
    private static final PoolConfig PAYMENT_TAPE_POOL =
            new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig("net_amount", 30), null, null);

    private ReadinessCheckContext context(PoolConfig pool, ConciliationRequirementsConfig conciliationRequirements) {
        return new ReadinessCheckContext(3L, DATE, "Colombia (COL)", 3L, pool, conciliationRequirements);
    }

    private ConciliationRequirementsConfig requirementsWithTolerance(Integer tolerance, ConciliationRequirementRule... rules) {
        return new ConciliationRequirementsConfig(List.of(new ConciliationRequirementGroup(List.of(rules))), tolerance);
    }

    private void stubWindow(List<PaymentTapeEntity> tapes) {
        when(workingDaysCalculator.subtractWorkingDays(eq(DATE), eq(30), anyString())).thenReturn(DATE.minusDays(30));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(any(), any(), any()))
                .thenReturn(tapes);
    }

    @Test
    void evaluate_nonPaymentTapeStrategy_passesWithoutQuerying() {
        var pool = new PoolConfig(PoolStrategyType.ACCOUNT_BALANCE, null, null, null);
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null));

        var result = check.evaluate(context(pool, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_nullConciliationRequirements_passesWithoutQuerying() {
        var result = check.evaluate(context(PAYMENT_TAPE_POOL, null));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_nullTolerancePercentage_passesWithoutQuerying() {
        var requirements = new ConciliationRequirementsConfig(List.of(new ConciliationRequirementGroup(
                List.of(new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null)))), null);

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_emptyGroups_passesWithoutQuerying() {
        var requirements = new ConciliationRequirementsConfig(List.of(), 10);

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(paymentTapeJPARepository, workingDaysCalculator);
    }

    @Test
    void evaluate_emptyPool_passes() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null));
        stubWindow(List.of());

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_allTapesReconciled_passes() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null));
        stubWindow(List.of(
                PaymentTapeEntity.builder().id("pt-1").companyId(3L).paymentId("p-1").build(),
                PaymentTapeEntity.builder().id("pt-2").companyId(3L).paymentId("p-2").build()));

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_nonReconciledUnderTolerance_passes() {
        // 1 de 10 sin conciliar = 10%, tolerancia 10% -> no supera (estrictamente mayor bloquea)
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null));
        var tapes = new java.util.ArrayList<PaymentTapeEntity>();
        for (int i = 0; i < 9; i++) {
            tapes.add(PaymentTapeEntity.builder().id("pt-" + i).companyId(3L).paymentId("p-" + i).build());
        }
        tapes.add(PaymentTapeEntity.builder().id("pt-unreconciled").companyId(3L).paymentId(null).build());
        stubWindow(tapes);

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_nonReconciledOverTolerance_fails() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null));
        stubWindow(List.of(
                PaymentTapeEntity.builder().id("pt-1").companyId(3L).paymentId(null).build(),
                PaymentTapeEntity.builder().id("pt-2").companyId(3L).paymentId("p-2").build()));

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
        assertThat(result.reason()).contains("50").contains("10%");
    }

    @Test
    void evaluate_fundsTransferRule_usesFundTransferIdColumn() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.FUNDS_TRANSFER, null));
        stubWindow(List.of(
                PaymentTapeEntity.builder().id("pt-1").companyId(3L).fundTransferId(null).build(),
                PaymentTapeEntity.builder().id("pt-2").companyId(3L).fundTransferId("ft-2").build()));

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
    }

    @Test
    void evaluate_groupsAreOred_satisfyingAnyGroupCountsAsReconciled() {
        var groupA = new ConciliationRequirementGroup(
                List.of(new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, null)));
        var groupB = new ConciliationRequirementGroup(
                List.of(new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.FUNDS_TRANSFER, null)));
        var requirements = new ConciliationRequirementsConfig(List.of(groupA, groupB), 10);
        // No cumple el grupo A (paymentId null) pero sí el grupo B (fundTransferId presente) -> reconciled
        stubWindow(List.of(PaymentTapeEntity.builder().id("pt-1").companyId(3L).paymentId(null).fundTransferId("ft-1").build()));

        var result = check.evaluate(context(PAYMENT_TAPE_POOL, requirements));

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_ruleWithSpecificGateway_throws() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS, "PayU"));
        stubWindow(List.of(PaymentTapeEntity.builder().id("pt-1").companyId(3L).build()));

        assertThatThrownBy(() -> check.evaluate(context(PAYMENT_TAPE_POOL, requirements)))
                .isInstanceOf(UnsupportedConciliationRuleException.class);
    }

    @Test
    void evaluate_unsupportedTablePair_throws() {
        var requirements = requirementsWithTolerance(10,
                new ConciliationRequirementRule(ConciliationTable.DISBURSEMENTS, ConciliationTable.BORROWER_CORE, null));
        stubWindow(List.of(PaymentTapeEntity.builder().id("pt-1").companyId(3L).build()));

        assertThatThrownBy(() -> check.evaluate(context(PAYMENT_TAPE_POOL, requirements)))
                .isInstanceOf(UnsupportedConciliationRuleException.class);
    }
}
