package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Etapa 3 — Payment Filters. {@code accountingPayments} (VPR-9631), {@code gatewayFilters}
 * (VPR-9632), {@code conciliationRequirements} (VPR-9633) y {@code dateTimeFilters} (VPR-9634) ya
 * están tipados — la etapa queda completa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DistributablePaymentsConfig(
        AccountingPaymentsConfig accountingPayments,
        GatewayFiltersConfig gatewayFilters,
        ConciliationRequirementsConfig conciliationRequirements,
        DateTimeFiltersConfig dateTimeFilters
) {}
