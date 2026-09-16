package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolFund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionOwnershipUseCaseTest {

    private final PartitionOwnershipUseCase useCase = new PartitionOwnershipUseCase();

    @Test
    void execute_fundWithRealOwner_isDistributable() {
        var fund = new PoolFund("pt-1", new BigDecimal("100.00"), "Owner Co");

        var result = useCase.execute(List.of(fund));

        assertThat(result.distributable()).containsExactly(fund);
        assertThat(result.ownerless()).isEmpty();
    }

    @Test
    void execute_fundWithUndefinedOwner_isOwnerless() {
        var fund = new PoolFund("pt-1", new BigDecimal("100.00"), ResolveOwnershipUseCase.UNDEFINED_OWNER);

        var result = useCase.execute(List.of(fund));

        assertThat(result.distributable()).isEmpty();
        assertThat(result.ownerless()).containsExactly(fund);
    }

    @Test
    void execute_mixOfBoth_partitionsCorrectly() {
        var owned = new PoolFund("pt-1", new BigDecimal("100.00"), "Owner Co");
        var ownerless = new PoolFund("pt-2", new BigDecimal("50.00"), ResolveOwnershipUseCase.UNDEFINED_OWNER);

        var result = useCase.execute(List.of(owned, ownerless));

        assertThat(result.distributable()).containsExactly(owned);
        assertThat(result.ownerless()).containsExactly(ownerless);
    }

    @Test
    void execute_emptyList_returnsBothEmpty() {
        var result = useCase.execute(List.of());

        assertThat(result.distributable()).isEmpty();
        assertThat(result.ownerless()).isEmpty();
    }
}
