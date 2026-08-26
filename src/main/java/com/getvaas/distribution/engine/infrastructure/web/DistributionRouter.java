package com.getvaas.distribution.engine.infrastructure.web;

import com.getvaas.distribution.engine.application.usecase.FetchCandidatePaymentTapesUseCase;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentTapeCandidateResponse;
import com.getvaas.security.annotation.VaasSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/distributions")
@RequiredArgsConstructor
public class DistributionRouter {

    private final FetchCandidatePaymentTapesUseCase fetchCandidatePaymentTapesUseCase;

    /** Diagnóstico: devuelve el pool candidato (Bloque 2a) sin ejecutar nada todavía. */
    @VaasSecurity
    @GetMapping("/candidates")
    public List<PaymentTapeCandidateResponse> getCandidates(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return fetchCandidatePaymentTapesUseCase.execute(companyId, date).stream()
                .map(PaymentTapeCandidateResponse::from)
                .toList();
    }
}
