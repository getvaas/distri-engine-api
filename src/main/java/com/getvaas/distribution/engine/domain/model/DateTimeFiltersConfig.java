package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Payment Filters — Date & Time Filters (VPR-9634). Lista plana de reglas independientes — a
 * diferencia de {@link AccountingPaymentsConfig}/{@link ConciliationRequirementsConfig}, esta
 * card no tiene AND/OR (confirmado contra el mockup y el HTML fuente).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DateTimeFiltersConfig(
        List<DateTimeFilterRule> rules
) {}
