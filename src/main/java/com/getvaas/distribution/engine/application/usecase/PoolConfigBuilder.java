package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountBalancePoolConfig;
import com.getvaas.distribution.engine.domain.model.AccountBalanceSource;
import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePoolConfigRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Construye la etapa Pool Strategy (VPR-9628 Payment Tape, VPR-9629 Account Balance), sin importar
 * la estrategia elegida — {@code DATA_SOURCE_AGGREGATION} (VPR-9630) sigue sin tipar hasta que un
 * deal la necesite.
 */
@Component
public class PoolConfigBuilder {

    private static final String DEFAULT_AMOUNT_FIELD = "net_amount";
    private static final int DEFAULT_DAYS_BACK = 90;
    private static final PoolBalanceType DEFAULT_BALANCE_TYPE = PoolBalanceType.USABLE_BALANCE;

    public PoolConfig build(UpdatePoolConfigRequest request) {
        var strategy = request.strategy() != null ? request.strategy() : PoolStrategyType.PAYMENT_TAPE;

        PaymentTapePoolConfig paymentTapeConfig = null;
        AccountBalancePoolConfig accountBalanceConfig = null;

        if (strategy == PoolStrategyType.PAYMENT_TAPE) {
            var daysBack = request.daysBack() != null ? request.daysBack() : DEFAULT_DAYS_BACK;
            if (daysBack < 0) {
                throw new InvalidDistributionConfigException("daysBack no puede ser negativo");
            }
            var amountField = request.amountField() != null ? request.amountField() : DEFAULT_AMOUNT_FIELD;
            paymentTapeConfig = new PaymentTapePoolConfig(amountField, daysBack);
        } else if (strategy == PoolStrategyType.ACCOUNT_BALANCE) {
            accountBalanceConfig = buildAccountBalanceConfig(request);
        }

        return new PoolConfig(strategy, paymentTapeConfig, accountBalanceConfig, null);
    }

    private AccountBalancePoolConfig buildAccountBalanceConfig(UpdatePoolConfigRequest request) {
        if (request.accounts() == null || request.accounts().isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "ACCOUNT_BALANCE requiere al menos una cuenta en 'accounts'");
        }

        var seenAccountIds = new HashSet<Long>();
        var accounts = request.accounts().stream()
                .map(a -> {
                    if (!seenAccountIds.add(a.accountId())) {
                        throw new InvalidDistributionConfigException(
                                "La cuenta " + a.accountId() + " está repetida en 'accounts'");
                    }
                    return new AccountBalanceSource(
                            a.accountId(),
                            a.balanceType() != null ? a.balanceType() : DEFAULT_BALANCE_TYPE,
                            a.description());
                })
                .toList();

        return new AccountBalancePoolConfig(accounts);
    }
}
