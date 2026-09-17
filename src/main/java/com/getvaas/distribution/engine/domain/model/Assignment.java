package com.getvaas.distribution.engine.domain.model;

import java.math.BigDecimal;

/**
 * Paso 7 del pipeline de ejecución (VPR-9668): cuánto le corresponde a un owner, ya calculado a
 * partir del pool distribuible y las reglas de Distribution Rules (VPR-9643). {@code owner} es la
 * etiqueta descriptiva (mismo patrón de string libre que {@link PoolFund#owner()} y
 * {@link ComponentOwnerRule#owner()} — para el remanente sin reclamar, {@code String.valueOf(companyId)}).
 * {@code accountId} es la cuenta real a la que se persiste el monto — {@link ComponentOwnerRule#toAccountId()}
 * para assignments de regla, o {@link RemainingBalanceConfig#destinationAccountId()} para el
 * remanente; nunca null en un {@code Assignment} ya construido (fallar antes si no se puede
 * resolver una cuenta real, ver {@code CalculateAssignmentsUseCase}).
 * {@code concept} (VPR-9669) es el texto libre para {@code assignment.concept} al persistir —
 * {@link ComponentOwnerRule#description()} si existe, si no {@code owner} — resuelto acá porque
 * {@code Assignment} no referencia la regla que lo originó.
 */
public record Assignment(
        String owner,
        Long accountId,
        String concept,
        BigDecimal amount
) {}
