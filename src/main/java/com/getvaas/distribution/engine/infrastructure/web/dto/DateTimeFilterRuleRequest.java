package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterOperator;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;

public record DateTimeFilterRuleRequest(
        String gateway,
        DateTimeFilterRuleType ruleType,
        DateTimeFilterOperator operator,
        String value,
        Integer maxDays
) {}
