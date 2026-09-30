package com.getvaas.distribution.engine.infrastructure.web.dto;

public record RemainingBalanceConfigRequest(
        Long destinationAccountId,
        Long fromAccountId
) {}
