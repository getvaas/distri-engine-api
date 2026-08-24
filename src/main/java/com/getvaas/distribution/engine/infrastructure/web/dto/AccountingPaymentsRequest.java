package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record AccountingPaymentsRequest(
        Boolean hasAccountingPayments,
        Boolean distributeAccountingPayments,
        List<PaymentFilterConditionGroupRequest> conditionGroups
) {}
