package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountingPaymentsConfig;
import com.getvaas.distribution.engine.domain.model.DateTimeFilterRule;
import com.getvaas.distribution.engine.domain.model.DateTimeFiltersConfig;
import com.getvaas.distribution.engine.domain.model.GatewayFiltersConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFilterCondition;
import com.getvaas.distribution.engine.domain.model.PaymentFilterConditionGroup;
import com.getvaas.distribution.engine.domain.model.PaymentFiltersConfig;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;
import com.getvaas.distribution.engine.domain.service.calendar.WorkingDaysCalculator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Bloque 2 del pipeline de ejecución (VPR-9664): aplica en runtime Accounting Payments (VPR-9631),
 * Gateway Filters (VPR-9632) y Date & Time Filters (VPR-9634) sobre los payment tapes que
 * sobrevivieron el gate de conciliación (VPR-9663). Cada filtro es independiente y se aplica en
 * secuencia — un tape debe sobrevivir los 3 para seguir en el pool.
 * <p>
 * Las condiciones de Accounting Payments solo evalúan columnas ya mapeadas en
 * {@link PaymentTapeEntity} — cualquier otro {@code field}, o un operador numérico (
 * {@code GT}/{@code LT}) sobre un campo no numérico, falla explícito
 * ({@link UnsupportedPaymentFilterFieldException}) en vez de evaluarse mal o ignorarse en silencio.
 */
@Component
@RequiredArgsConstructor
public class ApplyPaymentFiltersUseCase {

    private final WorkingDaysCalculator workingDaysCalculator;

    public List<PaymentTapeEntity> execute(PaymentFiltersConfig paymentFilters, List<PaymentTapeEntity> tapes,
            LocalDate date, String country) {
        if (paymentFilters == null) {
            return tapes;
        }

        var filtered = applyAccountingPaymentsFilter(paymentFilters.accountingPayments(), tapes);
        filtered = applyGatewayFilter(paymentFilters.gatewayFilters(), filtered);
        filtered = applyDateTimeFilters(paymentFilters.dateTimeFilters(), filtered, date, country);
        return filtered;
    }

    // ===== Accounting Payments (VPR-9631) =====

    private List<PaymentTapeEntity> applyAccountingPaymentsFilter(AccountingPaymentsConfig config, List<PaymentTapeEntity> tapes) {
        if (config == null || !config.hasAccountingPayments() || config.distributeAccountingPayments()) {
            return tapes;
        }
        return tapes.stream().filter(tape -> !matchesAnyGroup(tape, config.conditionGroups())).toList();
    }

    private boolean matchesAnyGroup(PaymentTapeEntity tape, List<PaymentFilterConditionGroup> groups) {
        return groups.stream().anyMatch(group -> group.conditions().stream().allMatch(condition -> matchesCondition(tape, condition)));
    }

    private boolean matchesCondition(PaymentTapeEntity tape, PaymentFilterCondition condition) {
        var value = resolveFieldValue(tape, condition.field());
        return switch (condition.operator()) {
            case IS_NULL -> value == null;
            case IS_NOT_NULL -> value != null;
            case EQ -> value != null && stringify(value).equals(condition.value());
            case NEQ -> value == null || !stringify(value).equals(condition.value());
            case CONTAINS -> value != null && stringify(value).contains(condition.value());
            case IN -> value != null && splitCsv(condition.value()).contains(stringify(value));
            case NOT_IN -> value == null || !splitCsv(condition.value()).contains(stringify(value));
            case GT -> compareNumeric(condition.field(), value, condition.value()) > 0;
            case LT -> compareNumeric(condition.field(), value, condition.value()) < 0;
        };
    }

    private int compareNumeric(String field, Object value, String conditionValue) {
        if (!(value instanceof BigDecimal amount)) {
            throw new UnsupportedPaymentFilterFieldException(
                    "GT/LT solo aplica a campos numéricos, no a '" + field + "'");
        }
        BigDecimal threshold;
        try {
            threshold = new BigDecimal(conditionValue);
        } catch (NumberFormatException e) {
            throw new UnsupportedPaymentFilterFieldException("valor no numérico para GT/LT: '" + conditionValue + "'");
        }
        return amount.compareTo(threshold);
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).toList();
    }

    private String stringify(Object value) {
        return value.toString();
    }

    private Object resolveFieldValue(PaymentTapeEntity tape, String field) {
        return switch (field) {
            case "id" -> tape.getId();
            case "company_id" -> tape.getCompanyId();
            case "payment_date" -> tape.getPaymentDate();
            case "distribution_id" -> tape.getDistributionId();
            case "payment_id" -> tape.getPaymentId();
            case "fund_transfer_id" -> tape.getFundTransferId();
            case "net_amount" -> tape.getNetAmount();
            // "gross_amount" no es una columna real de payment_tape — remapeado a total_payment,
            // la columna real más cercana (ver PaymentTapeEntity).
            case "gross_amount" -> tape.getTotalPayment();
            case "gateway_code" -> tape.getGatewayCode();
            default -> throw new UnsupportedPaymentFilterFieldException(
                    "El campo '" + field + "' no está mapeado todavía en payment_tape para Payment Filters");
        };
    }

    // ===== Gateway Filters (VPR-9632) =====

    private List<PaymentTapeEntity> applyGatewayFilter(GatewayFiltersConfig config, List<PaymentTapeEntity> tapes) {
        if (config == null || config.mode() == GatewayFilterMode.ALL) {
            return tapes;
        }
        var gateways = new HashSet<>(config.gateways());
        return switch (config.mode()) {
            case INCLUDE_ONLY -> tapes.stream().filter(t -> gateways.contains(t.getGatewayCode())).toList();
            case EXCLUDE -> tapes.stream().filter(t -> !gateways.contains(t.getGatewayCode())).toList();
            case ALL -> tapes;
        };
    }

    // ===== Date & Time Filters (VPR-9634) =====

    private List<PaymentTapeEntity> applyDateTimeFilters(DateTimeFiltersConfig config, List<PaymentTapeEntity> tapes,
            LocalDate date, String country) {
        if (config == null || config.rules() == null || config.rules().isEmpty()) {
            return tapes;
        }
        return tapes.stream()
                .filter(tape -> config.rules().stream().allMatch(rule -> ruleSatisfied(rule, tape, date, country)))
                .toList();
    }

    private boolean ruleSatisfied(DateTimeFilterRule rule, PaymentTapeEntity tape, LocalDate date, String country) {
        if (rule.gateway() != null && !rule.gateway().equals(tape.getGatewayCode())) {
            return true;
        }
        if (tape.getPaymentDate() == null) {
            return true;
        }
        return switch (rule.ruleType()) {
            case DISTRIBUTE_BY_DATE -> evaluateDateComparison(rule, tape, date);
            case DISTRIBUTE_BY_DATE_TIME -> evaluateDateTimeComparison(rule, tape, date);
            case DAYS_BACK_LIMIT -> evaluateDaysBackLimit(rule, tape, date, country);
        };
    }

    private LocalDate resolveReferenceDate(String value, LocalDate evaluationDate) {
        return "today".equalsIgnoreCase(value) ? evaluationDate : LocalDate.parse(value);
    }

    private boolean evaluateDateComparison(DateTimeFilterRule rule, PaymentTapeEntity tape, LocalDate evaluationDate) {
        var referenceDate = resolveReferenceDate(rule.value(), evaluationDate);
        var tapeDate = tape.getPaymentDate().toLocalDate();
        return rule.operator() == DateTimeFilterOperator.IS_BEFORE
                ? tapeDate.isBefore(referenceDate)
                : tapeDate.isAfter(referenceDate);
    }

    private boolean evaluateDateTimeComparison(DateTimeFilterRule rule, PaymentTapeEntity tape, LocalDate evaluationDate) {
        var referenceDate = resolveReferenceDate(rule.value(), evaluationDate);
        var referenceDateTime = rule.cutoffTime() != null
                ? LocalDateTime.of(referenceDate, rule.cutoffTime())
                : referenceDate.atStartOfDay();
        return rule.operator() == DateTimeFilterOperator.IS_BEFORE
                ? tape.getPaymentDate().isBefore(referenceDateTime)
                : tape.getPaymentDate().isAfter(referenceDateTime);
    }

    private boolean evaluateDaysBackLimit(DateTimeFilterRule rule, PaymentTapeEntity tape, LocalDate evaluationDate, String country) {
        var cutoff = Boolean.TRUE.equals(rule.businessDays())
                ? workingDaysCalculator.subtractWorkingDays(evaluationDate, rule.maxDays(), country)
                : evaluationDate.minusDays(rule.maxDays());
        return !tape.getPaymentDate().toLocalDate().isBefore(cutoff);
    }
}
