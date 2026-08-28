package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;

import java.math.BigDecimal;
import java.util.List;

public record BalanceStrategyConfigRequest(
        String amountField,
        BalanceSufficiencyStrategy sufficiencyStrategy,
        AmountDistributionStrategy distributionStrategy,
        BigDecimal distributionValue,
        List<AccountTransferRuleRequest> accountTransferRules
) {}
