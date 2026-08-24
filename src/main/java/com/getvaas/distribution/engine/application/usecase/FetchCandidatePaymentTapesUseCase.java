package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PaymentTapeCandidate;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Bloque 2 del pipeline de ejecución (VPR-9662): trae los payment tapes candidatos — dentro de la
 * ventana de días hábiles configurada en Pool Strategy y todavía no distribuidos. No aplica todavía
 * Payment Filters (VPR-9631-9634) ni el gate de conciliación (VPR-9633) — esos se agregan encima en
 * tickets siguientes.
 */
@Component
@RequiredArgsConstructor
public class FetchCandidatePaymentTapesUseCase {

    private static final int DEFAULT_DAYS_BACK = 90;

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final WorkingDaysCalculator workingDaysCalculator;
    private final PaymentTapeJPARepository paymentTapeJPARepository;

    public List<PaymentTapeCandidate> execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);

        int daysBack = DEFAULT_DAYS_BACK;
        var pool = config.config().pool();
        if (pool != null && pool.paymentTape() != null && pool.paymentTape().daysBack() != null) {
            daysBack = pool.paymentTape().daysBack();
        }

        var fromDate = workingDaysCalculator.subtractWorkingDays(date, daysBack, config.config().country());

        var entities = paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                companyId, fromDate.atStartOfDay(), date.atTime(23, 59, 59));

        return entities.stream()
                .map(e -> new PaymentTapeCandidate(e.getId(), e.getCompanyId(), e.getPaymentDate()))
                .toList();
    }
}
