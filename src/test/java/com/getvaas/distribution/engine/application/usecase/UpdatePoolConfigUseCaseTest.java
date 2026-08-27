package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountBalanceSourceRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePoolConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePoolConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdatePoolConfigUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null);

    private DistributionConfig existingDomain(DistributionConfigPayload payload) {
        return new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private void mockExisting(DistributionConfigPayload payload) {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain(payload));
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    @Test
    void execute_noFieldsProvided_defaultsToPaymentTapeWithNetAmountAnd90DaysBack() {
        mockExisting(new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null));

        useCase.execute("id-1", new UpdatePoolConfigRequest(null, null, null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        PoolConfig pool = captor.getValue().pool();
        assertThat(pool.strategy()).isEqualTo(PoolStrategyType.PAYMENT_TAPE);
        assertThat(pool.paymentTape().amountField()).isEqualTo("net_amount");
        assertThat(pool.paymentTape().daysBack()).isEqualTo(90);
    }

    @Test
    void execute_customAmountFieldAndDaysBack_usesProvidedValues() {
        mockExisting(EMPTY_PAYLOAD);

        useCase.execute("id-1", new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, "gross_amount", 30, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        PoolConfig pool = captor.getValue().pool();
        assertThat(pool.paymentTape().amountField()).isEqualTo("gross_amount");
        assertThat(pool.paymentTape().daysBack()).isEqualTo(30);
    }

    @Test
    void execute_negativeDaysBack_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);

        assertThatThrownBy(() -> useCase.execute("id-1",
                new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, null, -1, null)))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_missingId_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", new UpdatePoolConfigRequest(null, null, null, null)))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }

    @Test
    void execute_preservesDealInfoFields() {
        mockExisting(new DistributionConfigPayload("Mexico (MEX)", "MXN", null, null, null, null, null, null, null));

        useCase.execute("id-1", new UpdatePoolConfigRequest(null, null, null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Mexico (MEX)");
        assertThat(captor.getValue().currency()).isEqualTo("MXN");
    }

    // ===== ACCOUNT_BALANCE (VPR-9629) =====

    @Test
    void execute_accountBalanceWithAccounts_buildsAccountBalanceConfigAndNoPaymentTapeConfig() {
        mockExisting(EMPTY_PAYLOAD);
        var accounts = List.of(
                new AccountBalanceSourceRequest(1016974L, PoolBalanceType.CURRENT_BALANCE, "PayU Recaudo"),
                new AccountBalanceSourceRequest(931465L, null, "PayU Recaudo 2"));

        useCase.execute("id-1", new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        PoolConfig pool = captor.getValue().pool();
        assertThat(pool.strategy()).isEqualTo(PoolStrategyType.ACCOUNT_BALANCE);
        assertThat(pool.paymentTape()).isNull();
        assertThat(pool.accountBalance().accounts()).hasSize(2);
        assertThat(pool.accountBalance().accounts().get(0).balanceType()).isEqualTo(PoolBalanceType.CURRENT_BALANCE);
    }

    @Test
    void execute_accountBalanceWithoutExplicitBalanceType_defaultsToUsableBalance() {
        mockExisting(EMPTY_PAYLOAD);
        var accounts = List.of(new AccountBalanceSourceRequest(1016974L, null, "PayU Recaudo"));

        useCase.execute("id-1", new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().pool().accountBalance().accounts().get(0).balanceType())
                .isEqualTo(PoolBalanceType.USABLE_BALANCE);
    }

    @Test
    void execute_accountBalanceWithoutAccounts_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);

        assertThatThrownBy(() -> useCase.execute("id-1",
                new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, List.of())))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_accountBalanceWithDuplicateAccountId_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var accounts = List.of(
                new AccountBalanceSourceRequest(1016974L, null, "PayU Recaudo"),
                new AccountBalanceSourceRequest(1016974L, PoolBalanceType.CURRENT_BALANCE, "Duplicada"));

        assertThatThrownBy(() -> useCase.execute("id-1",
                new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts)))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_switchingFromPaymentTapeToAccountBalance_doesNotLeavePaymentTapeConfig() {
        mockExisting(EMPTY_PAYLOAD);
        var accounts = List.of(new AccountBalanceSourceRequest(1016974L, null, "PayU Recaudo"));

        useCase.execute("id-1", new UpdatePoolConfigRequest(PoolStrategyType.ACCOUNT_BALANCE, null, null, accounts));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().pool().paymentTape()).isNull();
        assertThat(captor.getValue().pool().accountBalance()).isNotNull();
    }
}
