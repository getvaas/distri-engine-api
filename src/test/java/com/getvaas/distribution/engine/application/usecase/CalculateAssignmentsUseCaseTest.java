package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.domain.model.ComponentOwnerRule;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.RemainingBalanceConfig;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.AccountBalanceJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AccountBalanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateAssignmentsUseCaseTest {

    private static final Long COMPANY_ID = 3L;

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    @Mock
    private AccountBalanceJPARepository accountBalanceJPARepository;
    private CalculateAssignmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CalculateAssignmentsUseCase(resolveActiveDistributionConfigUseCase,
                new CheckAccountBalanceSufficiencyUseCase(accountBalanceJPARepository));
    }

    private PoolFund fund(String sourceId, String amount) {
        return new PoolFund(sourceId, new BigDecimal(amount), "welli");
    }

    private ComponentOwnerRule rule(String owner, AmountDistributionStrategy strategy, String value) {
        var balanceStrategy = strategy == null ? null
                : new BalanceStrategyConfig(null, null, null, strategy,
                        value == null ? null : new BigDecimal(value), List.of());
        return new ComponentOwnerRule(PaymentComponent.PRINCIPAL, owner, null, balanceStrategy, false);
    }

    private ComponentOwnerRule ruleWithBalanceCheck(String owner, AmountDistributionStrategy strategy, String value,
                                                     BalanceSufficiencyStrategy sufficiencyStrategy, Long accountId) {
        var balanceStrategy = new BalanceStrategyConfig(null, sufficiencyStrategy, List.of(accountId), strategy,
                value == null ? null : new BigDecimal(value), List.of());
        return new ComponentOwnerRule(PaymentComponent.PRINCIPAL, owner, null, balanceStrategy, false);
    }

    private void mockAccountBalance(Long accountId, String currentBalance) {
        var balance = AccountBalanceEntity.builder().accountId(accountId)
                .currentBalance(new BigDecimal(currentBalance)).build();
        when(accountBalanceJPARepository.findFirstByAccountIdOrderByCreationDateDesc(accountId))
                .thenReturn(Optional.of(balance));
    }

    private void mockConfig(DistributionRulesConfig rulesConfig) {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, rulesConfig, null, null, null, null);
        var config = new DistributionConfig("id-1", "Deal", COMPANY_ID, 3L, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        when(resolveActiveDistributionConfigUseCase.execute(COMPANY_ID)).thenReturn(config);
    }

    @Test
    void execute_singleDefaultRule_getsEntirePool() {
        mockConfig(new DistributionRulesConfig(true, List.of(rule("lender", AmountDistributionStrategy.DEFAULT, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(new Assignment("lender", new BigDecimal("100.00")));
    }

    @Test
    void execute_percentageOfPoolAndDefault_splitsCorrectly() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("investor", AmountDistributionStrategy.PERCENTAGE_OF_POOL, "30"),
                rule("lender", AmountDistributionStrategy.DEFAULT, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "1000.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(
                new Assignment("investor", new BigDecimal("300.00")),
                new Assignment("lender", new BigDecimal("700.00")));
    }

    @Test
    void execute_fixedAmount_subtractsFromPool() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("fee-collector", AmountDistributionStrategy.FIXED_AMOUNT, "50.00"),
                rule("lender", AmountDistributionStrategy.DEFAULT, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "500.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(
                new Assignment("fee-collector", new BigDecimal("50.00")),
                new Assignment("lender", new BigDecimal("450.00")));
    }

    @Test
    void execute_percentageOfRemaining_cascadesInConfigOrder() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("first", AmountDistributionStrategy.PERCENTAGE_OF_REMAINING, "50"),
                rule("second", AmountDistributionStrategy.PERCENTAGE_OF_REMAINING, "50")), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "1000.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        // 250.00 quedan sin reclamar -> caen al owner por default (la company)
        assertThat(result).containsExactly(
                new Assignment("first", new BigDecimal("500.00")),
                new Assignment("second", new BigDecimal("250.00")),
                new Assignment(String.valueOf(COMPANY_ID), new BigDecimal("250.00")));
    }

    @Test
    void execute_proportionalWeight_splitsProportionallyAmongWeightedRules() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("a", AmountDistributionStrategy.PROPORTIONAL_WEIGHT, "1"),
                rule("b", AmountDistributionStrategy.PROPORTIONAL_WEIGHT, "3")), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "400.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(
                new Assignment("a", new BigDecimal("100.00")),
                new Assignment("b", new BigDecimal("300.00")));
    }

    @Test
    void execute_overAllocation_throws() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("a", AmountDistributionStrategy.PERCENTAGE_OF_POOL, "70"),
                rule("b", AmountDistributionStrategy.PERCENTAGE_OF_POOL, "70")), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(AssignmentAllocationExceedsPoolException.class);
    }

    @Test
    void execute_fixedAmountExceedsPoolEvenWithDefaultRule_throws() {
        // Regresión: sin el chequeo por-paso, la regla DEFAULT "absorbía" el negativo resultante
        // en vez de fallar explícito.
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("fee-collector", AmountDistributionStrategy.FIXED_AMOUNT, "50.00"),
                rule("lender", AmountDistributionStrategy.DEFAULT, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "30.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(AssignmentAllocationExceedsPoolException.class);
    }

    @Test
    void execute_multipleDefaultRules_throws() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("a", AmountDistributionStrategy.DEFAULT, null),
                rule("b", AmountDistributionStrategy.DEFAULT, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(AmbiguousDefaultDistributionStrategyException.class);
    }

    @Test
    void execute_noComponentOwnersConfigured_allPoolGoesToCompany() {
        mockConfig(new DistributionRulesConfig(false, List.of(), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "250.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(new Assignment(String.valueOf(COMPANY_ID), new BigDecimal("250.00")));
    }

    @Test
    void execute_unclaimedRemainder_fallsBackToRemainingBalanceOverride() {
        var remainingBalance = new RemainingBalanceConfig(PaymentComponent.GUARANTEE, 999L);
        mockConfig(new DistributionRulesConfig(true,
                List.of(rule("investor", AmountDistributionStrategy.PERCENTAGE_OF_POOL, "40")),
                remainingBalance));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).containsExactly(
                new Assignment("investor", new BigDecimal("40.00")),
                new Assignment("999", new BigDecimal("60.00")));
    }

    @Test
    void execute_missingDistributionValue_throwsInvalidDistributionConfig() {
        mockConfig(new DistributionRulesConfig(true,
                List.of(rule("investor", AmountDistributionStrategy.PERCENTAGE_OF_POOL, null)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sufficientBalanceOrStop_withEnoughBalance_assignsFullAmount() {
        mockAccountBalance(61L, "1000.00");
        mockConfig(new DistributionRulesConfig(true, List.of(
                ruleWithBalanceCheck("lender", AmountDistributionStrategy.FIXED_AMOUNT, "50.00",
                        BalanceSufficiencyStrategy.SUFFICIENT_BALANCE_OR_STOP, 61L)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).contains(new Assignment("lender", new BigDecimal("50.00")));
    }

    @Test
    void execute_sufficientBalanceOrStop_withInsufficientBalance_throws() {
        mockAccountBalance(61L, "10.00");
        mockConfig(new DistributionRulesConfig(true, List.of(
                ruleWithBalanceCheck("lender", AmountDistributionStrategy.FIXED_AMOUNT, "50.00",
                        BalanceSufficiencyStrategy.SUFFICIENT_BALANCE_OR_STOP, 61L)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(InsufficientAccountBalanceException.class);
    }

    @Test
    void execute_sufficientBalanceOrSkipAllBorrowers_withInsufficientBalance_skipsRuleOnly() {
        mockAccountBalance(61L, "10.00");
        mockConfig(new DistributionRulesConfig(true, List.of(
                ruleWithBalanceCheck("borrower", AmountDistributionStrategy.FIXED_AMOUNT, "50.00",
                        BalanceSufficiencyStrategy.SUFFICIENT_BALANCE_OR_SKIP_ALL_BORROWERS, 61L)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        // La regla se saltea (monto 0, sin assignment); el pool entero cae al owner por default.
        assertThat(result).containsExactly(new Assignment(String.valueOf(COMPANY_ID), new BigDecimal("100.00")));
    }

    @Test
    void execute_untilBalanceExhausted_capsAtAvailableBalance() {
        mockAccountBalance(61L, "30.00");
        mockConfig(new DistributionRulesConfig(true, List.of(
                ruleWithBalanceCheck("lender", AmountDistributionStrategy.FIXED_AMOUNT, "50.00",
                        BalanceSufficiencyStrategy.UNTIL_BALANCE_EXHAUSTED_WHILE_FITTING_PAYMENTS, 61L)), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        // Capado a los 30.00 disponibles; los 70.00 restantes del pool caen al owner por default.
        assertThat(result).containsExactly(
                new Assignment("lender", new BigDecimal("30.00")),
                new Assignment(String.valueOf(COMPANY_ID), new BigDecimal("70.00")));
    }

    @Test
    void execute_balanceCheckWithoutAccountIdsToCheck_throwsInvalidDistributionConfig() {
        var balanceStrategy = new BalanceStrategyConfig(null, BalanceSufficiencyStrategy.SUFFICIENT_BALANCE_OR_STOP,
                List.of(), AmountDistributionStrategy.FIXED_AMOUNT, new BigDecimal("50.00"), List.of());
        var rule = new ComponentOwnerRule(PaymentComponent.PRINCIPAL, "lender", null, balanceStrategy, false);
        mockConfig(new DistributionRulesConfig(true, List.of(rule), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        assertThatThrownBy(() -> useCase.execute(COMPANY_ID, funds))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_noSufficiencyStrategyConfigured_skipsBalanceCheckEntirely() {
        mockConfig(new DistributionRulesConfig(true, List.of(
                rule("lender", AmountDistributionStrategy.FIXED_AMOUNT, "50.00")), null));
        var funds = new PartitionedPoolFunds(List.of(fund("pt-1", "100.00")), List.of());

        var result = useCase.execute(COMPANY_ID, funds);

        assertThat(result).contains(new Assignment("lender", new BigDecimal("50.00")));
    }
}
