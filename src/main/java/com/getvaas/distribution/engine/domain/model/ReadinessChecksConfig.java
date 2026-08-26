package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Etapa 7 — Readiness Checks (VPR-9637 preconditions, VPR-9638 failure behavior). Cada check
 * habilitado es un {@link ReadinessCheckSetting} independiente — habilitar un check es incluirlo
 * en {@code checks}. Solo {@code BUSINESS_DAY} tiene una implementación real en
 * {@code ReadinessCheckRunner} hoy; los demás quedan {@code NOT_IMPLEMENTED} en runtime.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadinessChecksConfig(
        List<ReadinessCheckSetting> checks
) {}
