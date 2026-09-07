package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.EligiblePaymentTape;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bloque 2 del pipeline de ejecución (VPR-9662): trae los payment tapes elegibles — dentro de la
 * ventana de días hábiles configurada en Pool Strategy, todavía no distribuidos, con el monto ya
 * resuelto según {@code amountField} (VPR-9628). Pieza interna del motor de ejecución, consumida por
 * el orquestador (no expuesta por ningún endpoint — mismo criterio que {@link RunReadinessChecksUseCase}).
 * No aplica todavía Payment Filters (VPR-9631-9634) ni el gate de conciliación (VPR-9633) — esos se
 * agregan encima en tickets siguientes.
 */
@Component
@RequiredArgsConstructor
public class FetchEligiblePaymentTapesUseCase {

    private static final int DEFAULT_DAYS_BACK = 90;
    private static final String DEFAULT_AMOUNT_FIELD = "net_amount";

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final WorkingDaysCalculator workingDaysCalculator;
    private final PaymentTapeJPARepository paymentTapeJPARepository;

    public List<EligiblePaymentTape> execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var pool = config.config().pool();

        int daysBack = DEFAULT_DAYS_BACK;
        String amountField = DEFAULT_AMOUNT_FIELD;
        if (pool != null && pool.paymentTape() != null) {
            if (pool.paymentTape().daysBack() != null) {
                daysBack = pool.paymentTape().daysBack();
            }
            if (pool.paymentTape().amountField() != null) {
                amountField = pool.paymentTape().amountField();
            }
        }

        var fromDate = workingDaysCalculator.subtractWorkingDays(date, daysBack, config.config().country());

        var entities = paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                companyId, fromDate.atStartOfDay(), date.atTime(23, 59, 59));

        var resolvedAmountField = amountField;
        return entities.stream()
                .map(e -> new EligiblePaymentTape(e.getId(), e.getCompanyId(), e.getPaymentDate(),
                        resolveAmount(e, resolvedAmountField)))
                .toList();
    }

    private BigDecimal resolveAmount(PaymentTapeEntity entity, String amountField) {
        return switch (amountField) {
            case "net_amount" -> entity.getNetAmount();
            case "gross_amount" -> entity.getGrossAmount();
            default -> throw new UnsupportedPoolAmountFieldException(amountField);
        };
    }
}
