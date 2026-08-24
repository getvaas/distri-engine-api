package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import jakarta.validation.constraints.NotNull;

public record AccountBalanceSourceRequest(
        @NotNull Long accountId,
        PoolBalanceType balanceType,
        String description
) {}
