package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Solo existen 2 tipos reales, mapeados contra {@code account_balance} (columnas {@code current_balance}
 * y {@code projected_balance}) — la tercera opción del mockup ("Available - reserves") se descartó: las
 * "reserves" son en sí mismas un valor calculado, no algo restable como opción independiente.
 */
public enum PoolBalanceType {
    /** Saldo crudo, columna {@code current_balance}. */
    CURRENT_BALANCE,
    /** Saldo disponible para asignar: {@code projectedBalance ?: currentBalance}. */
    USABLE_BALANCE
}
