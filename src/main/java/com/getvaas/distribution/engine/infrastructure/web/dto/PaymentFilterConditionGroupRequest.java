package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record PaymentFilterConditionGroupRequest(
        List<PaymentFilterConditionRequest> conditions
) {}
