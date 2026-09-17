package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.BalanceStrategyConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.AccountBalanceJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chequeo de balance real de una {@code ComponentOwnerRule} (VPR-9668), verificado contra
 * {@code BalanceRule}/{@code BalanceStrategy} de {@code master-trust-servicer-api}. Consulta el
 * balance más reciente ({@code projectedBalance} si existe, si no {@code currentBalance}) de cada
 * cuenta en {@code accountIdsToCheck} y lo suma; una cuenta sin ningún registro de balance aporta
 * cero (el motor real tampoco sintetiza un default).
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

    public BigDecimal execute(BigDecimal claimedAmount, BalanceStrategyConfig balanceStrategy) {
        var strategy = balanceStrategy.sufficiencyStrategy();
        if (strategy == null) {
            return claimedAmount;
        }

        var accountIds = balanceStrategy.accountIdsToCheck();
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "'sufficiencyStrategy' requiere 'accountIdsToCheck' configurado");
        }

        var availableBalance = resolveAvailableBalance(accountIds);
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

    private BigDecimal resolveAvailableBalance(List<Long> accountIds) {
        return accountIds.stream()
                .map(this::usableBalanceOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal usableBalanceOf(Long accountId) {
        return accountBalanceJPARepository.findFirstByAccountIdOrderByCreationDateDesc(accountId)
                .map(balance -> balance.getProjectedBalance() != null
                        ? balance.getProjectedBalance()
                        : balance.getCurrentBalance())
                .orElse(BigDecimal.ZERO);
    }
}
