package com.getvaas.distribution.engine.domain.model;

/**
 * Pool Strategy cuando {@code strategy=PAYMENT_TAPE} (VPR-9628).
 * <p>
 * {@code amountField} no es un enum cerrado de 3 valores — es la columna real de {@code payment_tape}
 * usada para calcular el monto a transferir. {@code net_amount}/{@code gross_amount} son las columnas
 * típicas; cualquier otra columna real de la tabla es válida (decisión resuelta en VPR-9628).
 */
public record PaymentTapePoolConfig(
        String amountField,
        Integer daysBack
) {}
