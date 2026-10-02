package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountBalanceCheckTarget;
import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.AccountBalanceJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chequeo de balance real de una {@code ComponentOwnerRule} (VPR-9668), verificado contra
 * {@code BalanceRule}/{@code BalanceStrategy} de {@code master-trust-servicer-api}. Para cada cuenta
 * en {@code accountChecks}, deriva qué campo leer según su {@code AccountType}/{@code accountCode}
 * (ver {@link ResolveAccountBalanceFieldUseCase}) — {@code USABLE_BALANCE} = {@code projectedBalance}
 * si existe, si no {@code currentBalance}; {@code CURRENT_BALANCE} = siempre {@code currentBalance},
 * sin importar {@code projectedBalance} (hoy solo la excepción {@code WELLI_INVESTMENT}) — y lo suma;
 * una cuenta sin ningún registro de balance aporta cero (el motor real tampoco sintetiza un default).
 * <p>
 * Sin {@code sufficiencyStrategy} configurado, no hay chequeo — devuelve el monto reclamado tal
 * cual. {@code UNTIL_BALANCE_EXHAUSTED_WHILE_FITTING_PAYMENTS} real hace fitting greedy por
 * payment individual; acá, sin acceso a payments individuales en este punto del cálculo, se
 * simplifica a capar el monto al balance disponible (decisión explícita, VPR-9668).
 * {@code SUFFICIENT_BALANCE_OR_SKIP_ALL_BORROWERS} real salta todo el tier BORROWER (no
 * modelado); acá se simplifica a saltar solo esta regla (monto cero).
 */
@Component
@RequiredArgsConstructor
public class CheckAccountBalanceSufficiencyUseCase {

    private final AccountBalanceJPARepository accountBalanceJPARepository;
    private final ResolveAccountBalanceFieldUseCase resolveAccountBalanceFieldUseCase;

    public BigDecimal execute(BigDecimal claimedAmount, BalanceStrategyConfig balanceStrategy) {
        var strategy = balanceStrategy.sufficiencyStrategy();
        if (strategy == null) {
            return claimedAmount;
        }

        var accountChecks = balanceStrategy.accountChecks();
        if (accountChecks == null || accountChecks.isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "'sufficiencyStrategy' requiere 'accountChecks' configurado");
        }

        var availableBalance = resolveAvailableBalance(accountChecks);
        if (availableBalance.compareTo(claimedAmount) >= 0) {
            return claimedAmount;
        }

        return switch (strategy) {
            case SUFFICIENT_BALANCE_OR_STOP ->
                    throw new InsufficientAccountBalanceException(claimedAmount, availableBalance);
            case SUFFICIENT_BALANCE_OR_SKIP_ALL_BORROWERS -> BigDecimal.ZERO;
            case UNTIL_BALANCE_EXHAUSTED_WHILE_FITTING_PAYMENTS -> availableBalance.max(BigDecimal.ZERO);
        };
    }

    private BigDecimal resolveAvailableBalance(List<AccountBalanceCheckTarget> accountChecks) {
        return accountChecks.stream()
                .map(this::balanceOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal balanceOf(AccountBalanceCheckTarget accountCheck) {
        var balanceField = resolveAccountBalanceFieldUseCase.execute(accountCheck.accountType(), accountCheck.accountCode());
        return accountBalanceJPARepository.findFirstByAccountIdOrderByCreationDateDesc(accountCheck.accountId())
                .map(balance -> balanceField == PoolBalanceType.CURRENT_BALANCE
                        ? balance.getCurrentBalance()
                        : balance.getProjectedBalance() != null ? balance.getProjectedBalance() : balance.getCurrentBalance())
                .orElse(BigDecimal.ZERO);
    }
}
