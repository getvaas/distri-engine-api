package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;

public record ComponentOwnerRuleRequest(
        PaymentComponent component,
        String owner,
        String description
) {}
