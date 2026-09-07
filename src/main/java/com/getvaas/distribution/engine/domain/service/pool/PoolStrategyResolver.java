package com.getvaas.distribution.engine.domain.service.pool;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;

import java.time.LocalDate;
import java.util.List;

/**
 * Resuelve el pool de fondos elegibles para una company, según su Pool Strategy configurada
 * (VPR-9662). Cada estrategia es un {@code @Component} — {@link ResolveEligibleFundsUseCase} las
 * descubre todas automáticamente (mismo patrón que {@code ReadinessCheck}/{@code ReadinessCheckRunner}).
 */
public interface PoolStrategyResolver {

    PoolStrategyType type();

    List<PoolFund> resolve(Long companyId, LocalDate date);
}
