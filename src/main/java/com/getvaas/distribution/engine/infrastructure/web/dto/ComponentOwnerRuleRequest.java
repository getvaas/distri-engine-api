package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.PaymentType;

import java.util.List;

public record ComponentOwnerRuleRequest(
        String owner,
        String description,
        BalanceStrategyConfigRequest balanceStrategy,
        Boolean distributeAccountingPayments,
        Long toAccountId,
        Long fromAccountId,
        List<PaymentType> paymentTypes
) {}
