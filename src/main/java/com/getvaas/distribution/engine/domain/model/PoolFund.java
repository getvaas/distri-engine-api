package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fuente de fondos elegible para el pool de una distribución, sin importar qué Pool Strategy la
 * produjo — {@code sourceId} es el id del payment tape, la cuenta, o la fila de la fuente agregada,
 * según la estrategia (VPR-9662). {@code owner} (VPR-9665) es el owner ya resuelto — nunca null,
 * cae a {@code "UNDEFINED"} cuando no se puede resolver, para no bloquear el resto del pipeline.
 * {@code paymentDate} (VPR-9669) es la fecha del payment tape que originó el fondo — necesaria para
 * derivar {@code distribution.first_payment_date}/{@code last_payment_date} al persistir; se pierde
 * si no se propaga acá, porque el resto del pipeline solo trabaja con montos ya colapsados.
 */
public record PoolFund(
        String sourceId,
        BigDecimal amount,
        String owner,
        LocalDateTime paymentDate
) {}
