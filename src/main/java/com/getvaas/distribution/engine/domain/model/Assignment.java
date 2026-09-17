package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;

/**
 * Paso 7 del pipeline de ejecución (VPR-9668): cuánto le corresponde a un owner, ya calculado a
 * partir del pool distribuible y las reglas de Distribution Rules (VPR-9643). {@code owner} sigue
 * el mismo patrón de string libre que {@link PoolFund#owner()} y {@link ComponentOwnerRule#owner()}
 * — para el remanente sin reclamar por ninguna regla, es {@code String.valueOf(companyId)} (el
 * borrower de la distribución) o, si {@link RemainingBalanceConfig} está configurado,
 * {@code String.valueOf(destinationAccountId)}.
 */
public record Assignment(
        String owner,
        BigDecimal amount
) {}
