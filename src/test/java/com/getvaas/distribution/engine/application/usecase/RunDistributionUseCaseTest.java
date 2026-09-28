package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckOutcome;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunDistributionUseCaseTest {

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    @Mock
    private RunReadinessChecksUseCase runReadinessChecksUseCase;
    @Mock
    private ResolveEligibleFundsUseCase resolveEligibleFundsUseCase;
    @Mock
    private CalculateAssignmentsUseCase calculateAssignmentsUseCase;
    @Mock
    private PersistDistributionUseCase persistDistributionUseCase;
    @Mock
    private MarkPaymentTapesAsDistributedUseCase markPaymentTapesAsDistributedUseCase;
    @Mock
    private NotifyDistributionResultUseCase notifyDistributionResultUseCase;

    private RunDistributionUseCase useCase;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    private DistributionConfig activeConfig() {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, 3L, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @BeforeEach
    void setUp() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfig());
        lenient().when(persistDistributionUseCase.execute(any(), any(), any(), any()))
                .thenReturn(MasterServicerDistributionEntity.builder().id(99L).build());
        // PartitionOwnershipUseCase real (sin dependencias externas) — solo mockeamos lo que toca datos.
        useCase = new RunDistributionUseCase(resolveActiveDistributionConfigUseCase, runReadinessChecksUseCase,
                resolveEligibleFundsUseCase, new PartitionOwnershipUseCase(), calculateAssignmentsUseCase,
                persistDistributionUseCase, markPaymentTapesAsDistributedUseCase, notifyDistributionResultUseCase);
    }

    @Test
    void execute_ready_resolvesEligibleFunds() {
        var readiness = ReadinessCheckOutcome.of(List.of(
                new ReadinessCheckResult(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckStatus.PASSED, null)));
        when(runReadinessChecksUseCase.execute("id-1", DATE)).thenReturn(readiness);
        var funds = List.of(new PoolFund("pt-1", new BigDecimal("100.00"), "Owner Co", null));
        when(resolveEligibleFundsUseCase.execute(3L, DATE)).thenReturn(funds);
        var assignments = List.of(new Assignment("Owner Co", 61L, "Owner Co", new BigDecimal("100.00")));
        when(calculateAssignmentsUseCase.execute(eq(3L), any(PartitionedPoolFunds.class))).thenReturn(assignments);

        var result = useCase.execute(3L, DATE);

        assertThat(result.readiness().readyToDistribute()).isTrue();
        assertThat(result.funds().distributable()).isEqualTo(funds);
        assertThat(result.funds().ownerless()).isEmpty();
        assertThat(result.assignments()).isEqualTo(assignments);
        assertThat(result.distributionId()).isEqualTo(99L);
        verify(persistDistributionUseCase).execute(any(), eq(DATE), any(PartitionedPoolFunds.class), eq(assignments));
        verify(markPaymentTapesAsDistributedUseCase).execute(eq(3L), eq("99"), eq(funds));
        verify(notifyDistributionResultUseCase).execute(any(), eq(3L), eq(result));
    }

    @Test
    void execute_notReady_doesNotResolveFundsAndReturnsEmptyFunds() {
        var readiness = ReadinessCheckOutcome.of(List.of(
                new ReadinessCheckResult(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckStatus.FAILED, "fin de semana")));
        when(runReadinessChecksUseCase.execute("id-1", DATE)).thenReturn(readiness);

        var result = useCase.execute(3L, DATE);

        assertThat(result.readiness().readyToDistribute()).isFalse();
        assertThat(result.funds().distributable()).isEmpty();
        assertThat(result.funds().ownerless()).isEmpty();
        assertThat(result.assignments()).isEmpty();
        assertThat(result.distributionId()).isNull();
        verify(resolveEligibleFundsUseCase, never()).execute(anyLong(), any());
        verify(calculateAssignmentsUseCase, never()).execute(anyLong(), any());
        verify(persistDistributionUseCase, never()).execute(any(), any(), any(), any());
        verify(markPaymentTapesAsDistributedUseCase, never()).execute(anyLong(), anyString(), any());
        verify(notifyDistributionResultUseCase, never()).execute(any(), anyLong(), any());
    }

    @Test
    void execute_ready_partitionsOwnerlessFunds() {
        var readiness = ReadinessCheckOutcome.of(List.of(
                new ReadinessCheckResult(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckStatus.PASSED, null)));
        when(runReadinessChecksUseCase.execute("id-1", DATE)).thenReturn(readiness);
        var owned = new PoolFund("pt-1", new BigDecimal("100.00"), "Owner Co", null);
        var ownerless = new PoolFund("pt-2", new BigDecimal("50.00"), ResolveOwnershipUseCase.UNDEFINED_OWNER, null);
        when(resolveEligibleFundsUseCase.execute(3L, DATE)).thenReturn(List.of(owned, ownerless));

        var result = useCase.execute(3L, DATE);

        assertThat(result.funds().distributable()).containsExactly(owned);
        assertThat(result.funds().ownerless()).containsExactly(ownerless);
        verify(markPaymentTapesAsDistributedUseCase).execute(eq(3L), eq("99"), eq(List.of(owned)));
    }
}
