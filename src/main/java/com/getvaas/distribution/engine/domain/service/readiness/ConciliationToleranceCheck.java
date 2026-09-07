package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.application.usecase.UnsupportedConciliationRuleException;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementGroup;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementRule;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementsConfig;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * VPR-9663: gate de conciliación con tolerancia — evolución del filtro binario silencioso que hoy
 * usa {@code master-trust-servicer-api} (epica-distri-engine.md E8). Cuenta el % de payment tapes
 * del pool candidato que no cumplen ninguna de las reglas de {@code ConciliationRequirementsConfig}
 * configuradas (grupos en OR, reglas en AND dentro de cada grupo); si supera
 * {@code tolerancePercentage}, bloquea. Sin {@code tolerancePercentage} configurado, o sin reglas,
 * o si el deal no usa Pool Strategy PAYMENT_TAPE, pasa automático (opt-in por deal).
 * <p>
 * Solo evalúa reglas PAYMENT_TAPE vs PAYMENTS (columna {@code payment_id}) y PAYMENT_TAPE vs
 * FUNDS_TRANSFER (columna {@code fund_transfer_id}), sin {@code gateway} específico — cualquier otra
 * combinación configurada falla explícito ({@link UnsupportedConciliationRuleException}), no se
 * evalúa mal ni se ignora en silencio. El override auditado (`force` + motivo, épica pedido #5)
 * queda fuera de alcance — no existe todavía ningún mecanismo de override en el motor.
 */
@Component
@RequiredArgsConstructor
public class ConciliationToleranceCheck implements ReadinessCheck {

    private static final int DEFAULT_DAYS_BACK = 90;

    private final WorkingDaysCalculator workingDaysCalculator;
    private final PaymentTapeJPARepository paymentTapeJPARepository;

    @Override
    public ReadinessCheckType type() {
        return ReadinessCheckType.CONCILIATION_TOLERANCE;
    }

    @Override
    public ReadinessCheckResult evaluate(ReadinessCheckContext context) {
        var pool = context.pool();
        if (pool == null || pool.strategy() != PoolStrategyType.PAYMENT_TAPE) {
            return passed();
        }

        var conciliationRequirements = context.conciliationRequirements();
        if (conciliationRequirements == null || conciliationRequirements.tolerancePercentage() == null) {
            return passed();
        }

        var groups = conciliationRequirements.groups();
        if (groups == null || groups.isEmpty()) {
            return passed();
        }

        int daysBack = pool.paymentTape() != null && pool.paymentTape().daysBack() != null
                ? pool.paymentTape().daysBack()
                : DEFAULT_DAYS_BACK;
        var fromDate = workingDaysCalculator.subtractWorkingDays(context.date(), daysBack, context.country());
        var tapes = paymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
                context.companyId(), fromDate.atStartOfDay(), context.date().atTime(23, 59, 59));

        if (tapes.isEmpty()) {
            return passed();
        }

        long nonReconciledCount = tapes.stream().filter(tape -> !isReconciled(tape, groups)).count();
        double nonReconciledPercentage = 100.0 * nonReconciledCount / tapes.size();

        if (nonReconciledPercentage > conciliationRequirements.tolerancePercentage()) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.FAILED,
                    String.format("%.1f%% del pool sin conciliar (%d de %d), supera la tolerancia configurada de %d%%",
                            nonReconciledPercentage, nonReconciledCount, tapes.size(), conciliationRequirements.tolerancePercentage()));
        }
        return passed();
    }

    private boolean isReconciled(PaymentTapeEntity tape, List<ConciliationRequirementGroup> groups) {
        return groups.stream().anyMatch(group -> group.rules().stream().allMatch(rule -> ruleSatisfied(tape, rule)));
    }

    private boolean ruleSatisfied(PaymentTapeEntity tape, ConciliationRequirementRule rule) {
        if (rule.gateway() != null) {
            throw new UnsupportedConciliationRuleException(rule);
        }

        var tables = Set.of(rule.tableA(), rule.tableB());
        if (tables.equals(Set.of(ConciliationTable.PAYMENT_TAPE, ConciliationTable.PAYMENTS))) {
            return tape.getPaymentId() != null;
        }
        if (tables.equals(Set.of(ConciliationTable.PAYMENT_TAPE, ConciliationTable.FUNDS_TRANSFER))) {
            return tape.getFundTransferId() != null;
        }
        throw new UnsupportedConciliationRuleException(rule);
    }

    private ReadinessCheckResult passed() {
        return new ReadinessCheckResult(type(), ReadinessCheckStatus.PASSED, null);
    }
}
