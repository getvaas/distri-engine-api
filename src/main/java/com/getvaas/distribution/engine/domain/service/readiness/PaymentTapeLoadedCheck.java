package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VPR-9661: si el deal usa Pool Strategy {@code PAYMENT_TAPE}, verifica que haya al menos un
 * payment tape sin distribuir en la ventana configurada — mismo criterio de "candidato" que
 * {@code FetchCandidatePaymentTapesUseCase} (VPR-9662), sin invocarlo directamente (ese use case
 * resuelve "la config activa" por su cuenta; acá ya tenemos la config puntual en el context). Si el
 * deal usa {@code ACCOUNT_BALANCE} o {@code DATA_SOURCE_AGGREGATION}, no depende de payment tape
 * para nada — el check pasa automáticamente.
 */
@Component
@RequiredArgsConstructor
public class PaymentTapeLoadedCheck implements ReadinessCheck {

    private static final int DEFAULT_DAYS_BACK = 90;

    private final WorkingDaysCalculator workingDaysCalculator;
    private final PaymentTapeJPARepository paymentTapeJPARepository;

    @Override
    public ReadinessCheckType type() {
        return ReadinessCheckType.PAYMENT_TAPE_LOADED;
    }

    @Override
    public ReadinessCheckResult evaluate(ReadinessCheckContext context) {
        var pool = context.pool();
        if (pool == null || pool.strategy() != PoolStrategyType.PAYMENT_TAPE) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.PASSED, null);
        }

        int daysBack = pool.paymentTape() != null && pool.paymentTape().daysBack() != null
                ? pool.paymentTape().daysBack()
                : DEFAULT_DAYS_BACK;

        var fromDate = workingDaysCalculator.subtractWorkingDays(context.date(), daysBack, context.country());
        var candidates = paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                context.companyId(), fromDate.atStartOfDay(), context.date().atTime(23, 59, 59));

        if (candidates.isEmpty()) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.FAILED,
                    "No hay payment tapes sin distribuir para company " + context.companyId()
                            + " entre " + fromDate + " y " + context.date());
        }
        return new ReadinessCheckResult(type(), ReadinessCheckStatus.PASSED, null);
    }
}
