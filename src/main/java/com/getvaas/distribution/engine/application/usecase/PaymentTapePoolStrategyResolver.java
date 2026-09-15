package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.service.pool.PoolStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Pool Strategy PAYMENT_TAPE (VPR-9628): el pool son los payment tapes elegibles, cada uno con su
 * monto ya resuelto según {@code amountField}. Implementación de aplicación (no de domain) porque
 * depende de {@link FetchEligiblePaymentTapesUseCase}, otro use case — domain no puede depender de
 * application.
 */
@Component
@RequiredArgsConstructor
public class PaymentTapePoolStrategyResolver implements PoolStrategyResolver {

    private final FetchEligiblePaymentTapesUseCase fetchEligiblePaymentTapesUseCase;

    @Override
    public PoolStrategyType type() {
        return PoolStrategyType.PAYMENT_TAPE;
    }

    @Override
    public List<PoolFund> resolve(Long companyId, LocalDate date) {
        return fetchEligiblePaymentTapesUseCase.execute(companyId, date).stream()
                .map(tape -> new PoolFund(tape.id(), tape.amount()))
                .toList();
    }
}
