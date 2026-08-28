package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.DeductionPeriodicity;
import com.getvaas.distribution.engine.domain.model.enums.DeductionType;

import java.math.BigDecimal;

/**
 * Una comisión a descontar dentro de una {@link AccountTransferRule} (VPR-9704). {@code accountId}
 * es opcional: {@code null} significa que la deducción no se transfiere a ninguna cuenta (solo
 * reduce el monto disponible), un valor no-null significa que el monto deducido va a esa cuenta
 * destinataria — mismo patrón {@code Long} ya usado en
 * {@code AccountTransferRule.fromAccountIds}/{@code toAccountIds}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Deduction(
        String concept,
        DeductionType type,
        BigDecimal value,
        Long accountId,
        DeductionPeriodicity periodicity
) {}
