package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchCandidatePaymentTapesUseCaseTest {

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;

    private FetchCandidatePaymentTapesUseCase useCase;

    @BeforeEach
    void setUp() {
        // WorkingDaysCalculator real (sin dependencias externas) — solo mockeamos lo que toca datos.
        useCase = new FetchCandidatePaymentTapesUseCase(
                resolveActiveDistributionConfigUseCase, new WorkingDaysCalculator(), paymentTapeJPARepository);
    }

    private DistributionConfig activeConfigWithDaysBack(Integer daysBack) {
        var poolConfig = daysBack != null
                ? new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig("net_amount", daysBack), null, null)
                : null;
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                poolConfig, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, null, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @Test
    void execute_usesConfiguredDaysBackForTheWindow() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWithDaysBack(5));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of());

        useCase.execute(3L, LocalDate.of(2026, 8, 24)); // lunes

        // 5 días hábiles hacia atrás desde el lunes 24/8 -> lunes 17/8
        verify(paymentTapeJPARepository).findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                3L, LocalDate.of(2026, 8, 17).atStartOfDay(), LocalDate.of(2026, 8, 24).atTime(23, 59, 59));
    }

    @Test
    void execute_noPoolConfigured_usesDefault90DaysBack() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWithDaysBack(null));
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of());

        useCase.execute(3L, LocalDate.of(2026, 8, 24));

        verify(paymentTapeJPARepository).findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any());
    }

    @Test
    void execute_mapsEntitiesToDomainCandidates() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWithDaysBack(5));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0)).build();
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        var result = useCase.execute(3L, LocalDate.of(2026, 8, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("pt-1");
        assertThat(result.get(0).companyId()).isEqualTo(3L);
        assertThat(result.get(0).paymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
    }

    @Test
    void execute_noActiveConfig_propagatesException() {
        when(resolveActiveDistributionConfigUseCase.execute(3L))
                .thenThrow(new NoActiveDistributionConfigException(3L));

        assertThatThrownBy(() -> useCase.execute(3L, LocalDate.of(2026, 8, 24)))
                .isInstanceOf(NoActiveDistributionConfigException.class);
    }
}
