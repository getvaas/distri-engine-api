package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;

/**
 * Una fuente de fondos elegible para el pool de una distribución, sin importar qué Pool Strategy la
 * produjo — {@code sourceId} es el id del payment tape, la cuenta, o la fila de la fuente agregada,
 * según la estrategia (VPR-9662).
 */
public record PoolFund(
        String sourceId,
        BigDecimal amount
) {}
