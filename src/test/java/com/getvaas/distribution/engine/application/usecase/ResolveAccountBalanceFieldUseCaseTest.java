package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.AccountType;
import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveAccountBalanceFieldUseCaseTest {

    private final ResolveAccountBalanceFieldUseCase useCase = new ResolveAccountBalanceFieldUseCase();

    @Test
    void execute_collection_resolvesUsableBalance() {
        assertThat(useCase.execute(AccountType.COLLECTION, "ANY_CODE")).isEqualTo(PoolBalanceType.USABLE_BALANCE);
    }

    @Test
    void execute_reserve_resolvesUsableBalance() {
        assertThat(useCase.execute(AccountType.RESERVE, "ANY_CODE")).isEqualTo(PoolBalanceType.USABLE_BALANCE);
    }

    @Test
    void execute_investmentWithoutWelliException_resolvesUsableBalance() {
        assertThat(useCase.execute(AccountType.INVESTMENT, "SOME_OTHER_INVESTMENT_ACCOUNT"))
                .isEqualTo(PoolBalanceType.USABLE_BALANCE);
    }

    @Test
    void execute_welliInvestmentAccountCode_forcesCurrentBalanceRegardlessOfType() {
        assertThat(useCase.execute(AccountType.INVESTMENT, "WELLI_INVESTMENT"))
                .isEqualTo(PoolBalanceType.CURRENT_BALANCE);
    }

    @Test
    void execute_welliInvestmentAccountCode_forcesCurrentBalanceEvenForNonInvestmentType() {
        // La excepción es por código de cuenta, no por tipo — se respeta aunque el tipo no coincida.
        assertThat(useCase.execute(AccountType.COLLECTION, "WELLI_INVESTMENT"))
                .isEqualTo(PoolBalanceType.CURRENT_BALANCE);
    }

    @Test
    void execute_nullAccountCode_doesNotMatchWelliException() {
        assertThat(useCase.execute(AccountType.INVESTMENT, null)).isEqualTo(PoolBalanceType.USABLE_BALANCE);
    }
}
