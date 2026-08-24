package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;

public record PaymentFilterConditionRequest(
        String field,
        PaymentFilterOperator operator,
        String value
) {}
