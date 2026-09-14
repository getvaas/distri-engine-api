package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;

/**
 * Una fuente de fondos elegible para el pool de una distribución, sin importar qué Pool Strategy la
 * produjo — {@code sourceId} es el id del payment tape, la cuenta, o la fila de la fuente agregada,
 * según la estrategia (VPR-9662). {@code owner} (VPR-9665) es el owner ya resuelto — nunca null,
 * cae a {@code "UNDEFINED"} cuando no se puede resolver, para no bloquear el resto del pipeline.
 */
public record PoolFund(
        String sourceId,
        BigDecimal amount,
        String owner
) {}
