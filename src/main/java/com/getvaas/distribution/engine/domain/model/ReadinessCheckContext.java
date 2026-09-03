package com.getvaas.distribution.engine.domain.model;

import java.time.LocalDate;

/**
 * Datos disponibles para evaluar readiness checks. Crece a medida que se agreguen checks nuevos —
 * {@code masterTrustId} lo necesita {@code NoDuplicateDistributionCheck} (VPR-9661) y {@code pool}
 * lo necesita {@code PaymentTapeLoadedCheck} (VPR-9661).
 */
public record ReadinessCheckContext(
        Long companyId,
        LocalDate date,
        String country,
        Long masterTrustId,
        PoolConfig pool
) {}
