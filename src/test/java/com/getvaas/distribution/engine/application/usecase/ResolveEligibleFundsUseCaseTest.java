package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.domain.service.pool.PoolStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveEligibleFundsUseCaseTest {

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    private DistributionConfig configWithStrategy(PoolStrategyType strategy) {
        var poolConfig = strategy != null ? new PoolConfig(strategy, null, null, null) : null;
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                poolConfig, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, null, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private PoolStrategyResolver fakeResolver(PoolStrategyType type, List<PoolFund> result) {
        return new PoolStrategyResolver() {
            @Override
            public PoolStrategyType type() {
                return type;
            }

            @Override
            public List<PoolFund> resolve(Long companyId, LocalDate date) {
                return result;
            }
        };
    }

    @Test
    void execute_supportedStrategy_delegatesToRegisteredResolver() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(configWithStrategy(PoolStrategyType.PAYMENT_TAPE));
        var expected = List.of(new PoolFund("pt-1", new BigDecimal("100.00")));
        var useCase = new ResolveEligibleFundsUseCase(
                resolveActiveDistributionConfigUseCase, List.of(fakeResolver(PoolStrategyType.PAYMENT_TAPE, expected)));

        var result = useCase.execute(3L, DATE);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void execute_strategyWithoutRegisteredResolver_throws() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(configWithStrategy(PoolStrategyType.ACCOUNT_BALANCE));
        var useCase = new ResolveEligibleFundsUseCase(
                resolveActiveDistributionConfigUseCase, List.of(fakeResolver(PoolStrategyType.PAYMENT_TAPE, List.of())));

        assertThatThrownBy(() -> useCase.execute(3L, DATE))
                .isInstanceOf(UnsupportedPoolStrategyException.class);
    }

    @Test
    void execute_noPoolConfigured_throws() {
        when(resolveActiveDistributionConfigUseCase.execute(3L)).thenReturn(configWithStrategy(null));
        var useCase = new ResolveEligibleFundsUseCase(
                resolveActiveDistributionConfigUseCase, List.of(fakeResolver(PoolStrategyType.PAYMENT_TAPE, List.of())));

        assertThatThrownBy(() -> useCase.execute(3L, DATE))
                .isInstanceOf(UnsupportedPoolStrategyException.class);
    }
}
