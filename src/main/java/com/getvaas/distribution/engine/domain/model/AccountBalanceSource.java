package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;

public record AccountBalanceSource(
        Long accountId,
        PoolBalanceType balanceType,
        String description
) {}
