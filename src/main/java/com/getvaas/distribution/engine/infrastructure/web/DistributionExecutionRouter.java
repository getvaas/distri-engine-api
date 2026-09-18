package com.getvaas.distribution.engine.infrastructure.web;

import com.getvaas.distribution.engine.application.usecase.RunDistributionUseCase;
import com.getvaas.distribution.engine.domain.model.DistributionExecutionResult;
import com.getvaas.distribution.engine.infrastructure.web.dto.RunDistributionRequest;
import com.getvaas.security.annotation.VaasSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Disparo manual de una corrida del motor de ejecución (VPR-9669+) — mismo tipo de acción que el
 * endpoint real {@code POST /master-trusts/distributions/manual} de
 * {@code master-trust-servicer-api} (confirmado investigando VPR-9669), no una pieza de
 * diagnóstico interno como el endpoint de candidatos que se sacó en VPR-9662.
 * <p>
 * Alcance provisional, pensado para pruebas manuales mientras no existe scheduler: falta
 * confirmar autorización real más allá de {@code @VaasSecurity}, idempotencia (correr dos veces
 * el mismo día no está protegido más allá de {@code NO_DUPLICATE_DISTRIBUTION}, si está
 * habilitado), y si la corrida debería ser síncrona o async. No tratar como contrato final.
 */
@RestController
@RequestMapping("/distributions")
@RequiredArgsConstructor
public class DistributionExecutionRouter {

    private final RunDistributionUseCase runDistributionUseCase;

    @VaasSecurity
    @PostMapping("/run")
    public DistributionExecutionResult run(@Valid @RequestBody RunDistributionRequest request) {
        var date = request.date() != null ? request.date() : LocalDate.now();
        return runDistributionUseCase.execute(request.companyId(), date);
    }
}
