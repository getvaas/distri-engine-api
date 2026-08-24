package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.PaymentTapeCandidate;

import java.time.LocalDateTime;

public record PaymentTapeCandidateResponse(
        String id,
        Long companyId,
        LocalDateTime paymentDate
) {
    public static PaymentTapeCandidateResponse from(PaymentTapeCandidate candidate) {
        return new PaymentTapeCandidateResponse(candidate.id(), candidate.companyId(), candidate.paymentDate());
    }
}
