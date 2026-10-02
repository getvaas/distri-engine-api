package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.AccountType;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountBalanceSourceRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePoolConfigRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoolConfigBuilderTest {

    private final PoolConfigBuilder builder = new PoolConfigBuilder();

    @Test
    void build_noFieldsProvided_defaultsToPaymentTapeWithNetAmountAnd90DaysBack() {
        PoolConfig pool = builder.build(new UpdatePoolConfigRequest(null, null, null, null));

        assertThat(pool.strategy()).isEqualTo(PoolStrategyType.PAYMENT_TAPE);
        assertThat(pool.paymentTape().amountField()).isEqualTo("net_amount");
        assertThat(pool.paymentTape().daysBack()).isEqualTo(90);
    }

    @Test
    void build_customAmountFieldAndDaysBack_usesProvidedValues() {
        PoolConfig pool = builder.build(new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, "gross_amount", 30, null));

        assertThat(pool.paymentTape().amountField()).isEqualTo("gross_amount");
        assertThat(pool.paymentTape().daysBack()).isEqualTo(30);
    }

    @Test
    void build_negativeDaysBack_throwsInvalidDistributionConfigException() {
        assertThatThrownBy(() -> builder.build(new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, null, -1, null)))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    // ===== ACCOUNT_BALANCE (VPR-9629) =====

    @Test
    void build_accountBalanceWithAccounts_buildsAccountBalanceConfigAndNoPaymentTapeConfig() {
        var accounts = List.of(
                new AccountBalanceSourceRequest(1016974L, AccountType.INVESTMENT, "WELLI_INVESTMENT", "PayU Recaudo"),
                new AccountBalanceSourceRequest(931465L, AccountType.COLLECTION, null, "PayU Recaudo 2"));

        PoolConfig pool = builder.build(new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts));

        assertThat(pool.strategy()).isEqualTo(PoolStrategyType.ACCOUNT_BALANCE);
        assertThat(pool.paymentTape()).isNull();
        assertThat(pool.accountBalance().accounts()).hasSize(2);
        assertThat(pool.accountBalance().accounts().get(0).accountType()).isEqualTo(AccountType.INVESTMENT);
        assertThat(pool.accountBalance().accounts().get(0).accountCode()).isEqualTo("WELLI_INVESTMENT");
    }

    @Test
    void build_accountBalanceWithoutAccounts_throwsInvalidDistributionConfigException() {
        assertThatThrownBy(() -> builder.build(new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, List.of())))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_accountBalanceWithDuplicateAccountId_throwsInvalidDistributionConfigException() {
        var accounts = List.of(
                new AccountBalanceSourceRequest(1016974L, AccountType.COLLECTION, null, "PayU Recaudo"),
                new AccountBalanceSourceRequest(1016974L, AccountType.RESERVE, null, "Duplicada"));

        assertThatThrownBy(() -> builder.build(new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts)))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_switchingFromPaymentTapeToAccountBalance_doesNotLeavePaymentTapeConfig() {
        var accounts = List.of(new AccountBalanceSourceRequest(1016974L, AccountType.COLLECTION, null, "PayU Recaudo"));

        PoolConfig pool = builder.build(new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts));

        assertThat(pool.paymentTape()).isNull();
        assertThat(pool.accountBalance()).isNotNull();
    }
}
