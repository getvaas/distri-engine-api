package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record AccountTransferRuleRequest(
        List<Long> fromAccountIds,
        List<Long> toAccountIds,
        PaymentFilterConditionRequest condition
) {}
