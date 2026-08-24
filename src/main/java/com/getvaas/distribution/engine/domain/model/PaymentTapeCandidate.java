package com.getvaas.distribution.engine.domain.model;

import java.time.LocalDateTime;

public record PaymentTapeCandidate(
        String id,
        Long companyId,
        LocalDateTime paymentDate
) {}
