package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.service.pool.PoolStrategyResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Punto de entrada único del motor de ejecución para resolver el pool de fondos elegibles de una
 * distribución (VPR-9662): resuelve la config ACTIVE de la company, evalúa qué Pool Strategy tiene
 * configurada, y delega en el {@link PoolStrategyResolver} registrado para esa estrategia. Si la
 * estrategia no tiene resolver todavía (ACCOUNT_BALANCE, DATA_SOURCE_AGGREGATION), falla explícito
 * en vez de devolver un pool vacío o incorrecto — no se puede seguir procesando sin saber calcular
 * el pool real.
 */
@Component
public class ResolveEligibleFundsUseCase {

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final Map<PoolStrategyType, PoolStrategyResolver> resolversByType;

    public ResolveEligibleFundsUseCase(
            ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase,
            List<PoolStrategyResolver> resolvers) {
        this.resolveActiveDistributionConfigUseCase = resolveActiveDistributionConfigUseCase;
        this.resolversByType = resolvers.stream()
                .collect(Collectors.toMap(PoolStrategyResolver::type, Function.identity()));
    }

    public List<PoolFund> execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var pool = config.config().pool();
        var strategy = pool != null ? pool.strategy() : null;

        var resolver = resolversByType.get(strategy);
        if (resolver == null) {
            throw new UnsupportedPoolStrategyException(strategy);
        }

        return resolver.resolve(companyId, date);
    }
}
