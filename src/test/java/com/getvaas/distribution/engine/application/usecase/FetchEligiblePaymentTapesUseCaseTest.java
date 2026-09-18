package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipSourceConfig;
import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchEligiblePaymentTapesUseCaseTest {

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;

    private FetchEligiblePaymentTapesUseCase useCase;

    @BeforeEach
    void setUp() {
        // WorkingDaysCalculator/ApplyPaymentFiltersUseCase reales (sin dependencias externas) — solo
        // mockeamos lo que toca datos. Los configs de este test no traen paymentFilters, así que el
        // filtro es un no-op.
        var workingDaysCalculator = new WorkingDaysCalculator();
        useCase = new FetchEligiblePaymentTapesUseCase(
                resolveActiveDistributionConfigUseCase, workingDaysCalculator, paymentTapeJPARepository,
                new ApplyPaymentFiltersUseCase(workingDaysCalculator), new ResolveOwnershipUseCase());
    }

    private DistributionConfig activeConfigWithDaysBack(Integer daysBack) {
        return activeConfigWith(daysBack, "net_amount");
    }

    private DistributionConfig activeConfigWith(Integer daysBack, String amountField) {
        return activeConfigWith(daysBack, amountField, null);
    }

    private DistributionConfig activeConfigWith(Integer daysBack, String amountField, OwnershipConfig ownership) {
        var poolConfig = daysBack != null
                ? new PoolConfig(PoolStrategyType.PAYMENT_TAPE, new PaymentTapePoolConfig(amountField, daysBack), null, null)
                : null;
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                poolConfig, null, null, null, ownership, null, null, null);
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
    void execute_mapsEntitiesToDomainEligibleTapes() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWithDaysBack(5));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0))
                .netAmount(new BigDecimal("100.50")).build();
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        var result = useCase.execute(3L, LocalDate.of(2026, 8, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("pt-1");
        assertThat(result.get(0).companyId()).isEqualTo(3L);
        assertThat(result.get(0).paymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
        assertThat(result.get(0).amount()).isEqualByComparingTo("100.50");
        assertThat(result.get(0).owner()).isEqualTo(ResolveOwnershipUseCase.UNDEFINED_OWNER);
    }

    @Test
    void execute_ownershipConfigured_resolvesOwnerFromPaymentTapeField() {
        var ownership = new OwnershipConfig(new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null), null);
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWith(5, "net_amount", ownership));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0))
                .netAmount(new BigDecimal("100.50")).ownerName("Somos SAS").build();
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        var result = useCase.execute(3L, LocalDate.of(2026, 8, 24));

        assertThat(result.get(0).owner()).isEqualTo("Somos SAS");
    }

    @Test
    void execute_amountFieldGrossAmount_usesTotalPaymentColumn() {
        // "gross_amount" no es una columna real de payment_tape — se remapea a total_payment.
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWith(5, "gross_amount"));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0))
                .netAmount(new BigDecimal("90.00")).totalPayment(new BigDecimal("100.00")).build();
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        var result = useCase.execute(3L, LocalDate.of(2026, 8, 24));

        assertThat(result.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void execute_unsupportedAmountField_throws() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWith(5, "fee_amount"));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0)).build();
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        assertThatThrownBy(() -> useCase.execute(3L, LocalDate.of(2026, 8, 24)))
                .isInstanceOf(UnsupportedPoolAmountFieldException.class);
    }

    @Test
    void execute_amountFieldNullOnSpecificTape_throws() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(activeConfigWith(5, "net_amount"));
        var entity = PaymentTapeEntity.builder().id("pt-1").companyId(3L)
                .paymentDate(LocalDateTime.of(2026, 8, 20, 10, 0)).build(); // netAmount null
        when(paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                eq(3L), any(), any())).thenReturn(List.of(entity));

        assertThatThrownBy(() -> useCase.execute(3L, LocalDate.of(2026, 8, 24)))
                .isInstanceOf(NullAmountFieldValueException.class);
    }

    @Test
    void execute_noActiveConfig_propagatesException() {
        when(resolveActiveDistributionConfigUseCase.execute(3L))
                .thenThrow(new NoActiveDistributionConfigException(3L));

        assertThatThrownBy(() -> useCase.execute(3L, LocalDate.of(2026, 8, 24)))
                .isInstanceOf(NoActiveDistributionConfigException.class);
    }
}
