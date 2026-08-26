package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipMismatchStrategy;

/**
 * Ownership — Cross Validation (VPR-9636). Cruza el owner declarado en el payment tape contra la
 * Ownership API como control independiente. Las 4 estrategias completas (E1) están en alcance,
 * aunque hoy solo {@code API_WINS} esté validado en producción (Finkargo Colombia). Cuando
 * {@code enabled=false}, {@code mismatchStrategy} no aplica.
 * <p>
 * Riesgos documentados, no resueltos aquí: el fallback si la Ownership API externa no responde
 * (timeout/reintentos); que {@code BLOCK_PAYMENT} reuse la partición de ownerless ya existente
 * ({@code DISTRIBUTION_UNKNOWN_OWNER_PAYMENTS}) es una decisión de la etapa de ejecución.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnershipCrossValidationConfig(
        boolean enabled,
        OwnershipMismatchStrategy mismatchStrategy
) {}
