package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Paso 10c del pipeline de ejecución (Bloque 5, VPR-9669): marca cada payment tape distribuido con
 * el {@code distributionId} real ya persistido — mismo dato que {@code PaymentTapeDataProvider.save()}
 * en el motor real. Solo toca {@code distributable}, nunca {@code ownerless} (esos fondos siguen
 * siendo candidatos en la próxima corrida). Datasource propio (`payments_db`), sin transacción
 * distribuida con la persistencia de la distribución — ver {@link PersistDistributionUseCase}.
 */
@Component
@RequiredArgsConstructor
public class MarkPaymentTapesAsDistributedUseCase {

    private final PaymentTapeJPARepository paymentTapeJPARepository;

    @Transactional("paymentsTransactionManager")
    public void execute(Long companyId, String distributionId, List<PoolFund> distributable) {
        for (var fund : distributable) {
            var tape = paymentTapeJPARepository.findById(new PaymentTapeId(fund.sourceId(), companyId))
                    .orElseThrow(() -> new PaymentTapeNotFoundException(fund.sourceId(), companyId));
            tape.setDistributionId(distributionId);
            paymentTapeJPARepository.save(tape);
        }
    }
}
