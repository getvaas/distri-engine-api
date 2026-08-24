package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;

import java.util.List;

public record UpdatePoolConfigRequest(
        PoolStrategyType strategy,
        String amountField,
        Integer daysBack,
        List<AccountBalanceSourceRequest> accounts
) {}
