package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;

public record RemainingBalanceConfigRequest(
        PaymentComponent component,
        Long destinationAccountId
) {}
