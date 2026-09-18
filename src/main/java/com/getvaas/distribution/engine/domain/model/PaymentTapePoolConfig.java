package com.getvaas.distribution.engine.domain.model;

/**
 * Pool Strategy cuando {@code strategy=PAYMENT_TAPE} (VPR-9628).
 * <p>
 * {@code amountField} no es un enum cerrado — es la columna real de {@code payment_tape} usada para
 * calcular el monto a transferir. Solo {@code net_amount} y {@code gross_amount} están soportados
 * hoy (cualquier otro valor falla explícito, {@code UnsupportedPoolAmountFieldException}) —
 * {@code gross_amount} no es una columna real de la tabla (verificado contra el schema real), se
 * remapea a {@code total_payment} (ver {@code PaymentTapeEntity}).
 */
public record PaymentTapePoolConfig(
        String amountField,
        Integer daysBack
) {}
