package com.getvaas.distribution.engine.application.usecase;

/**
 * VPR-9666: si el {@code amountField} configurado (columna real y soportada de {@code payment_tape})
 * viene {@code null} en una tape puntual, esa tape falla explícito en vez de sumar un monto
 * incorrecto o dejar pasar un {@code null} en silencio al pool. El fallback de 3 niveles real del
 * motor actual (`netAmount` → `totalPayment - feeAmount` → `totalPayment`) no se generaliza acá:
 * `fee_amount` está confirmado siempre {@code null} hoy (bug del extractor) y `total_payment` no es
 * una columna verificada en este repo — mismo principio que el guardrail de moneda (E9): fallar
 * explícito en vez de calcular en base a datos que no se pueden confiar.
 */
public class NullAmountFieldValueException extends RuntimeException {

    public NullAmountFieldValueException(String tapeId, String amountField) {
        super("El payment tape '" + tapeId + "' tiene '" + amountField + "' null — no se puede resolver su monto");
    }
}
