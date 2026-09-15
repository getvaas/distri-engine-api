package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Payment Filters — Conciliation Requirements (VPR-9633). Un deal puede no tener ningún
 * requerimiento (lista vacía es válida) — a diferencia de {@link AccountingPaymentsConfig}, no hay
 * toggle que obligue a configurar algo.
 * <p>
 * {@code tolerancePercentage} (VPR-9663, 0-100, nullable) es el % máximo de payment tapes del pool
 * candidato que puede quedar sin conciliar antes de bloquear la distribución. {@code null} = sin
 * gate de tolerancia configurado — comportamiento binario sin cambios (opt-in por deal).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConciliationRequirementsConfig(
        List<ConciliationRequirementGroup> groups,
        Integer tolerancePercentage
) {}
