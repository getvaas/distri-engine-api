package com.getvaas.distribution.engine.application.usecase;

/**
 * {@code PaymentFilterCondition.field} no es un enum cerrado (VPR-9631: "cualquier columna real de
 * payment_tape es válida"), pero {@code PaymentTapeEntity} solo mapea las columnas que ya se
 * necesitaron hasta ahora — mismo criterio incremental que {@code amountField} (VPR-9628). Pedir un
 * campo real pero todavía no mapeado, o un operador que no tiene sentido para ese campo (ej.
 * {@code GT}/{@code LT} sobre una columna no numérica), falla explícito.
 */
public class UnsupportedPaymentFilterFieldException extends RuntimeException {

    public UnsupportedPaymentFilterFieldException(String message) {
        super(message);
    }
}
