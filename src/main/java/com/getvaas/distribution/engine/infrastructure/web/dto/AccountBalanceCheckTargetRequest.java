package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.AccountType;
import jakarta.validation.constraints.NotNull;

public record AccountBalanceCheckTargetRequest(
        @NotNull Long accountId,
        @NotNull AccountType accountType,
        String accountCode
) {}
