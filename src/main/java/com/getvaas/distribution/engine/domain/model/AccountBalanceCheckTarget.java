package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.AccountType;

/**
 * Una cuenta a chequear en {@code BalanceStrategyConfig.accountChecks} (VPR-9668 original,
 * extendido): reemplaza el {@code List<Long>} plano de antes — cada cuenta ahora lleva su propio
 * {@link AccountType} y {@code accountCode}, para que {@code CheckAccountBalanceSufficiencyUseCase}
 * derive automáticamente qué campo de balance leer (ver {@code ResolveAccountBalanceFieldUseCase})
 * en vez de aplicar la misma fórmula fija a todas las cuentas por igual. Mismo patrón que
 * {@link AccountBalanceSource} (Pool Strategy {@code ACCOUNT_BALANCE}), que ya era una lista de
 * objetos con info propia por cuenta.
 */
public record AccountBalanceCheckTarget(
        Long accountId,
        AccountType accountType,
        String accountCode
) {}
