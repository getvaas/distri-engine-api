package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Etapa 6 — Ownership. {@code source} (VPR-9635) y {@code crossValidation} (VPR-9636) ya están
 * tipados. Ambos son opcionales — un deal puede no tener ninguno configurado todavía.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnershipConfig(
        OwnershipSourceConfig source,
        OwnershipCrossValidationConfig crossValidation
) {}
