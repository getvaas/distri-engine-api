package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;

/**
 * Una regla de Conciliation Requirements: la conciliación entre {@code tableA} y {@code tableB} es
 * requerida para poder distribuir. {@code gateway} nulo significa "All gateways" — campo
 * independiente por regla, no comparte estado con {@link GatewayFiltersConfig}.
 * <p>
 * Riesgo abierto (no resuelto aquí): el motor real de conciliación (`ConciliationType` en
 * master-trust-servicer-api) no distingue {@code FUNDS_TRANSFER} de {@code DISBURSEMENTS} como
 * conceptos propios — ambos quedan implícitos en un chequeo genérico "Payments vs Bank". Cómo se
 * verifica cada combinación contra el motor real se resuelve en Readiness Checks, no aquí.
 */
public record ConciliationRequirementRule(
        ConciliationTable tableA,
        ConciliationTable tableB,
        String gateway
) {}
