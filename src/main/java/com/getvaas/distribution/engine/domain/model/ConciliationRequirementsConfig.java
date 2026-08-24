package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Payment Filters — Conciliation Requirements (VPR-9633). Un deal puede no tener ningún
 * requerimiento (lista vacía es válida) — a diferencia de {@link AccountingPaymentsConfig}, no hay
 * toggle que obligue a configurar algo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConciliationRequirementsConfig(
        List<ConciliationRequirementGroup> groups
) {}
