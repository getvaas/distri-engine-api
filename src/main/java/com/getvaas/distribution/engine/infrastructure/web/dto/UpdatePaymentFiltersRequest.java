package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record UpdatePaymentFiltersRequest(
        AccountingPaymentsRequest accountingPayments,
        UpdateGatewayFiltersRequest gatewayFilters,
        List<ConciliationRequirementGroupRequest> conciliationRequirements,
        Integer conciliationTolerancePercentage,
        List<DateTimeFilterRuleRequest> dateTimeFilters
) {}
