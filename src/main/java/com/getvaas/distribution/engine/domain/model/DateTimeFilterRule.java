package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;

import java.time.LocalTime;

/**
 * Una regla de Date & Time Filters. {@code gateway} nulo significa "All gateways". Según
 * {@code ruleType}: {@code DISTRIBUTE_BY_DATE}/{@code DISTRIBUTE_BY_DATE_TIME} usan
 * {@code operator} + {@code value} ({@code "today"} o fecha ISO absoluta — ambos tipos comparten
 * el mismo mecanismo de comparación, solo cambia la granularidad); {@code cutoffTime} (VPR-9664,
 * nullable) afina la comparación con una hora límite del día, solo tiene sentido para
 * {@code DISTRIBUTE_BY_DATE_TIME}. {@code DAYS_BACK_LIMIT} usa {@code maxDays} y
 * {@code businessDays} (VPR-9664, nullable — {@code true} cuenta días hábiles vía
 * {@code WorkingDaysCalculator}, {@code null}/{@code false} cuenta corridos). El "Days back" global
 * de Pool Strategy (VPR-9628) es el mismo mecanismo que {@code DAYS_BACK_LIMIT} aquí, con esta
 * regla permitiendo una excepción más fina por gateway sobre ese default — la relación se resuelve
 * en la etapa de ejecución, no aquí.
 */
public record DateTimeFilterRule(
        String gateway,
        DateTimeFilterRuleType ruleType,
        DateTimeFilterOperator operator,
        String value,
        Integer maxDays,
        LocalTime cutoffTime,
        Boolean businessDays
) {}
