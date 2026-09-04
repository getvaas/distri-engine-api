package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

/**
 * Habilita un readiness check con su propio comportamiento de falla (VPR-9637/9638). Cada check
 * tiene su propio {@code failureAction}/{@code retry} — no uno global para toda la config, porque
 * los borrowers reales usan los 3 modos (Inklusiva particiona-y-sigue, Finamco bloquea-todo,
 * Rapicredit solo-reporta) y no es válido simplificar a uno solo por default.
 * <p>
 * {@code forceRunOnNonBusinessDay} es un campo reservado (VPR-9661) — solo aplica a
 * {@code type=BUSINESS_DAY}, se persiste tal cual pero {@code BusinessDayCheck} todavía no lo lee;
 * el override real (forzar distribución en día no hábil) queda para un ticket futuro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadinessCheckSetting(
        ReadinessCheckType type,
        ReadinessCheckFailureAction failureAction,
        ReadinessCheckRetry retry,
        Boolean forceRunOnNonBusinessDay
) {}
