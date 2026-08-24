package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record UpdatePaymentFiltersRequest(
        AccountingPaymentsRequest accountingPayments,
        UpdateGatewayFiltersRequest gatewayFilters,
        List<ConciliationRequirementGroupRequest> conciliationRequirements,
        List<DateTimeFilterRuleRequest> dateTimeFilters
) {}
