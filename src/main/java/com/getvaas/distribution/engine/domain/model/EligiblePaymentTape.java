package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * {@code columns} (VPR-9698) carga cada columna real de {@code PaymentTapeEntity} ya resuelta y
 * disponible para esta fila (hoy: {@code net_amount}, {@code gross_amount} — ver
 * {@code FetchEligiblePaymentTapesUseCase.resolveColumns}), además del {@code amount} escalar ya
 * elegido para el pool — permite que una regla de Distribution Rules (estrategia
 * {@code SUM_COLUMN}) sume una columna distinta a la del pool. Una fila sin valor para una columna
 * dada simplemente no la incluye en el mapa (no hay entrada {@code null}).
 */
public record EligiblePaymentTape(
        String id,
        Long companyId,
        LocalDateTime paymentDate,
        BigDecimal amount,
        String owner,
        Map<String, BigDecimal> columns
) {}
