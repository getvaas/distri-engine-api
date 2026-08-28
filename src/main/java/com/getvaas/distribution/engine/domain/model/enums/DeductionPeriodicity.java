package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Con qué frecuencia se cobra una {@code Deduction} (VPR-9704). El cálculo real de si ya se
 * cobró en el período correspondiente es responsabilidad de la etapa de ejecución (Pista B).
 */
public enum DeductionPeriodicity {
    ALWAYS,
    ONCE_PER_DISTRIBUTION,
    ONCE_PER_MONTH,
    ONCE_PER_WEEK
}
