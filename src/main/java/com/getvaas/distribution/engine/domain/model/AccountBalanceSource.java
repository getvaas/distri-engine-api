package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.AccountType;

/**
 * Una cuenta del Pool Strategy {@code ACCOUNT_BALANCE} (VPR-9629). {@code accountType}/
 * {@code accountCode} (reemplazan el {@code balanceType: PoolBalanceType} manual de antes) llegan
 * ya resueltos por el caller — ver {@code ResolveAccountBalanceFieldUseCase} para cómo se derivan a
 * qué campo de balance corresponde leer. Sin resolver de ejecución todavía para esta estrategia de
 * pool (ver {@code ResolveEligibleFundsUseCase}) — este cambio es solo de config, por consistencia.
 */
public record AccountBalanceSource(
        Long accountId,
        AccountType accountType,
        String accountCode,
        String description
) {}
