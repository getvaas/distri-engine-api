package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountBalanceCheckTarget;
import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.domain.model.enums.AccountType;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.AccountBalanceJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AccountBalanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckAccountBalanceSufficiencyUseCaseTest {

    private static final Long COLLECTION_ACCOUNT_ID = 61L;
    private static final Long WELLI_ACCOUNT_ID = 62L;

    @Mock
    private AccountBalanceJPARepository accountBalanceJPARepository;

    private CheckAccountBalanceSufficiencyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CheckAccountBalanceSufficiencyUseCase(accountBalanceJPARepository,
                new ResolveAccountBalanceFieldUseCase());
    }

    private void mockBalance(Long accountId, String currentBalance, String projectedBalance) {
        var balance = AccountBalanceEntity.builder()
                .accountId(accountId)
                .currentBalance(new BigDecimal(currentBalance))
                .projectedBalance(projectedBalance == null ? null : new BigDecimal(projectedBalance))
                .build();
        when(accountBalanceJPARepository.findFirstByAccountIdOrderByCreationDateDesc(accountId))
                .thenReturn(Optional.of(balance));
    }

    private BalanceStrategyConfig balanceStrategy(List<AccountBalanceCheckTarget> accountChecks) {
        return new BalanceStrategyConfig(null, BalanceSufficiencyStrategy.SUFFICIENT_BALANCE_OR_STOP,
                accountChecks, AmountDistributionStrategy.FIXED_AMOUNT, new BigDecimal("100.00"), List.of());
    }

    @Test
    void execute_collectionAccount_usesProjectedOverCurrent() {
        mockBalance(COLLECTION_ACCOUNT_ID, "10.00", "100.00");
        var strategy = balanceStrategy(List.of(
                new AccountBalanceCheckTarget(COLLECTION_ACCOUNT_ID, AccountType.COLLECTION, null)));

        var result = useCase.execute(new BigDecimal("100.00"), strategy);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void execute_welliInvestmentAccount_ignoresProjectedUsesCurrent() {
        // projected=1000 alcanzaría, pero WELLI fuerza current=10 -> no alcanza, se rechaza.
        mockBalance(WELLI_ACCOUNT_ID, "10.00", "1000.00");
        var strategy = balanceStrategy(List.of(
                new AccountBalanceCheckTarget(WELLI_ACCOUNT_ID, AccountType.INVESTMENT, "WELLI_INVESTMENT")));

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("100.00"), strategy))
                .isInstanceOf(InsufficientAccountBalanceException.class);
    }

    @Test
    void execute_mixedAccounts_eachResolvesItsOwnBalanceField() {
        // collection: projected=50 (usableBalance). welli: current=60, projected=9999 ignorado.
        mockBalance(COLLECTION_ACCOUNT_ID, "5.00", "50.00");
        mockBalance(WELLI_ACCOUNT_ID, "60.00", "9999.00");
        var strategy = balanceStrategy(List.of(
                new AccountBalanceCheckTarget(COLLECTION_ACCOUNT_ID, AccountType.COLLECTION, null),
                new AccountBalanceCheckTarget(WELLI_ACCOUNT_ID, AccountType.INVESTMENT, "WELLI_INVESTMENT")));

        // Total disponible = 50 (collection, projected) + 60 (welli, current forzado) = 110.
        var result = useCase.execute(new BigDecimal("110.00"), strategy);

        assertThat(result).isEqualByComparingTo("110.00");
    }

    @Test
    void execute_investmentAccountWithoutWelliException_usesUsableBalanceLikeCollection() {
        mockBalance(COLLECTION_ACCOUNT_ID, "5.00", "100.00");
        var strategy = balanceStrategy(List.of(
                new AccountBalanceCheckTarget(COLLECTION_ACCOUNT_ID, AccountType.INVESTMENT, "SOME_OTHER_ACCOUNT")));

        var result = useCase.execute(new BigDecimal("100.00"), strategy);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void execute_withoutAccountChecks_throwsInvalidDistributionConfig() {
        var strategy = balanceStrategy(List.of());

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("100.00"), strategy))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_accountWithNoBalanceRecord_contributesZero() {
        when(accountBalanceJPARepository.findFirstByAccountIdOrderByCreationDateDesc(COLLECTION_ACCOUNT_ID))
                .thenReturn(Optional.empty());
        var strategy = balanceStrategy(List.of(
                new AccountBalanceCheckTarget(COLLECTION_ACCOUNT_ID, AccountType.COLLECTION, null)));

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("1.00"), strategy))
                .isInstanceOf(InsufficientAccountBalanceException.class);
    }
}
