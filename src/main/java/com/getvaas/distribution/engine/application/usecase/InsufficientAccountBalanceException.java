package com.getvaas.distribution.engine.application.usecase;

import java.math.BigDecimal;

/**
 * Estrategia {@code SUFFICIENT_BALANCE_OR_STOP} (VPR-9668): el balance disponible en las cuentas
 * de {@code accountIdsToCheck} no alcanza para el monto que la regla necesita mover. Falla
 * explícito y frena la corrida entera, igual que el motor real.
 */
public class InsufficientAccountBalanceException extends RuntimeException {

    public InsufficientAccountBalanceException(BigDecimal requiredAmount, BigDecimal availableBalance) {
        super("Balance insuficiente: se requieren " + requiredAmount + " pero las cuentas configuradas "
                + "tienen " + availableBalance + " disponibles");
    }
}
