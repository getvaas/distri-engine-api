package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;

/**
 * Etapa 2 — Pool Strategy. {@code paymentTape} (VPR-9628) y {@code accountBalance} (VPR-9629) ya están
 * tipados. {@code dataSourceAggregation} (VPR-9630) queda como JSON crudo hasta que se implemente —
 * mismo patrón que el resto de secciones TBD del payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PoolConfig(
        PoolStrategyType strategy,
        PaymentTapePoolConfig paymentTape,
        AccountBalancePoolConfig accountBalance,
        JsonNode dataSourceAggregation
) {}
