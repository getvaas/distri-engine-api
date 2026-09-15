package com.getvaas.distribution.engine.application.usecase;

/**
 * {@code amountField} no es un enum cerrado (VPR-9628: "cualquier columna real de payment_tape es
 * válida"), pero {@code PaymentTapeEntity} solo mapea las columnas de monto que ya se necesitaron
 * hasta ahora — mapeo incremental, mismo criterio que el resto de la entity. Pedir un campo real
 * pero todavía no mapeado falla explícito en vez de devolver un monto incorrecto o null en silencio.
 */
public class UnsupportedPoolAmountFieldException extends RuntimeException {

    public UnsupportedPoolAmountFieldException(String amountField) {
        super("amountField '" + amountField + "' no está soportado todavía — columnas de monto "
                + "mapeadas: net_amount, gross_amount");
    }
}
