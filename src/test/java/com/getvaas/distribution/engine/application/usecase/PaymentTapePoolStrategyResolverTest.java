package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.EligiblePaymentTape;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTapePoolStrategyResolverTest {

    @Mock
    private FetchEligiblePaymentTapesUseCase fetchEligiblePaymentTapesUseCase;
    @InjectMocks
    private PaymentTapePoolStrategyResolver resolver;

    @Test
    void type_isPaymentTape() {
        assertThat(resolver.type()).isEqualTo(PoolStrategyType.PAYMENT_TAPE);
    }

    @Test
    void resolve_mapsEligibleTapesToPoolFunds() {
        var tape = new EligiblePaymentTape("pt-1", 3L, LocalDateTime.now(), new BigDecimal("100.00"), "Owner Co");
        when(fetchEligiblePaymentTapesUseCase.execute(3L, LocalDate.of(2026, 8, 24)))
                .thenReturn(List.of(tape));

        var result = resolver.resolve(3L, LocalDate.of(2026, 8, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sourceId()).isEqualTo("pt-1");
        assertThat(result.get(0).amount()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).owner()).isEqualTo("Owner Co");
    }
}
