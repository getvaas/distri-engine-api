package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkPaymentTapesAsDistributedUseCaseTest {

    private static final Long COMPANY_ID = 3L;

    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;
    @InjectMocks
    private MarkPaymentTapesAsDistributedUseCase useCase;

    @Test
    void execute_marksEachDistributableFundAsDistributed() {
        var tape = PaymentTapeEntity.builder().id("pt-1").companyId(COMPANY_ID)
                .netAmount(new BigDecimal("100.00")).build();
        when(paymentTapeJPARepository.findById(new PaymentTapeId("pt-1", COMPANY_ID)))
                .thenReturn(Optional.of(tape));
        var fund = new PoolFund("pt-1", new BigDecimal("100.00"), "lender", null);

        useCase.execute(COMPANY_ID, "42", List.of(fund));

        var captor = ArgumentCaptor.forClass(PaymentTapeEntity.class);
        verify(paymentTapeJPARepository).save(captor.capture());
        assertThat(captor.getValue().getDistributionId()).isEqualTo("42");
    }

    @Test
    void execute_noDistributableFunds_savesNothing() {
        useCase.execute(COMPANY_ID, "42", List.of());

        verify(paymentTapeJPARepository, never()).save(any());
    }

    @Test
    void execute_paymentTapeNotFound_throws() {
        when(paymentTapeJPARepository.findById(new PaymentTapeId("pt-1", COMPANY_ID)))
                .thenReturn(Optional.empty());
        var fund = new PoolFund("pt-1", new BigDecimal("100.00"), "lender", null);

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, "42", List.of(fund)))
                .isInstanceOf(PaymentTapeNotFoundException.class);
    }
}
