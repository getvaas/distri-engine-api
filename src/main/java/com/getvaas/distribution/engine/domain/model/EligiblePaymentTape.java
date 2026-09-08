package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EligiblePaymentTape(
        String id,
        Long companyId,
        LocalDateTime paymentDate,
        BigDecimal amount
) {}
