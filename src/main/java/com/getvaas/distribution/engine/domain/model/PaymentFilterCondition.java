package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;

/**
 * Una condición del builder de Payment Filters. {@code field} es cualquier columna real del
 * payment tape (mismo patrón abierto que {@code amountField} en {@link PaymentTapePoolConfig}), no
 * un enum cerrado. {@code value} se persiste crudo — para {@code IN}/{@code NOT_IN} es una lista
 * separada por comas (ej. {@code "Siniestro, write-off"}); el parseo a lista ocurre en la etapa de
 * ejecución, no aquí. {@code value} es {@code null} para {@code IS_NULL}/{@code IS_NOT_NULL}.
 */
public record PaymentFilterCondition(
        String field,
        PaymentFilterOperator operator,
        String value
) {}
