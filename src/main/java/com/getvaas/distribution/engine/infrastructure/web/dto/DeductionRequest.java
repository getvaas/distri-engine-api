package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.DeductionPeriodicity;
import com.getvaas.distribution.engine.domain.model.enums.DeductionType;

import java.math.BigDecimal;

public record DeductionRequest(
        String concept,
        DeductionType type,
        BigDecimal value,
        Long accountId,
        DeductionPeriodicity periodicity
) {}
