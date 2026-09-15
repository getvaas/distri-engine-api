package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;

public class UnsupportedPoolStrategyException extends RuntimeException {

    public UnsupportedPoolStrategyException(PoolStrategyType strategy) {
        super(describe(strategy));
    }

    private static String describe(PoolStrategyType strategy) {
        if (strategy == null) {
            return "La distribution config no tiene Pool Strategy configurada";
        }
        return switch (strategy) {
            case ACCOUNT_BALANCE -> "Pool Strategy ACCOUNT_BALANCE todavía no está implementada "
                    + "(el pool son las cuentas configuradas, el monto es la suma de sus saldos) — "
                    + "falta la query real de account_balance";
            case DATA_SOURCE_AGGREGATION -> "Pool Strategy DATA_SOURCE_AGGREGATION todavía no está "
                    + "implementada (el monto final es la suma de todas las fuentes) — su config "
                    + "sigue sin tipar";
            case PAYMENT_TAPE -> "Pool Strategy PAYMENT_TAPE no tiene un PoolStrategyResolver registrado";
        };
    }
}
