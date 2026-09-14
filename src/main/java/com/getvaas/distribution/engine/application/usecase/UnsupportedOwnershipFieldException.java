package com.getvaas.distribution.engine.application.usecase;

/**
 * {@code OwnershipSourceConfig.field} no es un enum cerrado (soporta incluso rutas JSON, VPR-9635),
 * pero {@code PaymentTapeEntity} solo mapea {@code owner_name} para {@code PAYMENT_TAPE_FIELD} por
 * ahora — mismo criterio incremental que {@code amountField}. Pedir cualquier otro campo falla
 * explícito en vez de resolver un owner incorrecto o null en silencio.
 */
public class UnsupportedOwnershipFieldException extends RuntimeException {

    public UnsupportedOwnershipFieldException(String field) {
        super("field '" + field + "' no está soportado todavía para PAYMENT_TAPE_FIELD — solo se soporta 'owner_name'");
    }
}
