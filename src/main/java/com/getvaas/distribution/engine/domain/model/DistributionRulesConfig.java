package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Etapa 5 — Distribution Rules. Primera iteración mínima configurable (VPR-9643): owner por
 * componente de la cuota. {@code hasComponentOwners} (VPR-9699) declara si el deal usa esta
 * asignación o no — no hay validación cruzada con {@code componentOwners} al guardar, mismo
 * criterio del proyecto que permite drafts parciales. Fees/deducciones, multi-moneda por regla,
 * remanente/cascada e impuestos/seguros quedan explícitamente fuera — pendientes para historias
 * futuras.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DistributionRulesConfig(
        boolean hasComponentOwners,
        List<ComponentOwnerRule> componentOwners
) {}
