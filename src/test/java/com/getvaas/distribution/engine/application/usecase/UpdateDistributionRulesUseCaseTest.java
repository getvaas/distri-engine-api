package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.enums.AmountDistributionStrategy;
import com.getvaas.distribution.engine.domain.model.enums.BalanceSufficiencyStrategy;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.BalanceStrategyConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDistributionRulesUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateDistributionRulesUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

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

    private DistributionRulesConfig captureSavedRules() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue().rules();
    }

    @Test
    void execute_rulesForAll4Components_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.INTEREST, "funder", null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.LATE_FEE, "servicer", "late fees go to servicer", null),
                new ComponentOwnerRuleRequest(PaymentComponent.GUARANTEE, "guarantee_fund", null, null)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.componentOwners()).hasSize(4);
        assertThat(saved.componentOwners().get(2).owner()).isEqualTo("servicer");
        assertThat(saved.componentOwners().get(2).description()).isEqualTo("late fees go to servicer");
    }

    @Test
    void execute_ruleWithoutComponent_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(null, "funder", null, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_ruleWithoutOwner_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, null, null, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_duplicateComponent_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);

        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null),
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "servicer", null, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_emptyOrMissingRules_persistsEmptyListWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(false, null);

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.componentOwners()).isEmpty();
    }

    @Test
    void execute_preservesRestOfPayload() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null, null);
        mockExisting(existingPayload);
        var request = new UpdateDistributionRulesRequest(null, null);

        useCase.execute("id-1", request);

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }

    @Test
    void execute_ruleWithBalanceStrategy_persists() {
        mockExisting(EMPTY_PAYLOAD);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.UNTIL_EXHAUSTED, AmountDistributionStrategy.PERCENTAGE_OF_POOL,
                new BigDecimal("25.5"));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules().componentOwners().get(0).balanceStrategy();
        assertThat(saved.amountField()).isEqualTo("net_amount");
        assertThat(saved.sufficiencyStrategy()).isEqualTo(BalanceSufficiencyStrategy.UNTIL_EXHAUSTED);
        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.PERCENTAGE_OF_POOL);
        assertThat(saved.distributionValue()).isEqualByComparingTo("25.5");
    }

    @Test
    void execute_ruleWithoutBalanceStrategy_persistsAsNull() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules().componentOwners().get(0).balanceStrategy();
        assertThat(saved).isNull();
    }

    @Test
    void execute_fixedAmountWithoutDistributionValue_persistsWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.IGNORE_BALANCE, AmountDistributionStrategy.FIXED_AMOUNT, null);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules().componentOwners().get(0).balanceStrategy();
        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.FIXED_AMOUNT);
        assertThat(saved.distributionValue()).isNull();
    }

    @Test
    void execute_defaultWithDistributionValueSet_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var balanceStrategy = new BalanceStrategyConfigRequest("net_amount",
                BalanceSufficiencyStrategy.SUFFICIENT_OR_STOP, AmountDistributionStrategy.DEFAULT,
                new BigDecimal("100"));
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, balanceStrategy)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules().componentOwners().get(0).balanceStrategy();
        assertThat(saved.distributionStrategy()).isEqualTo(AmountDistributionStrategy.DEFAULT);
        assertThat(saved.distributionValue()).isEqualByComparingTo("100");
    }

    @Test
    void execute_hasComponentOwnersTrue_persists() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.hasComponentOwners()).isTrue();
    }

    @Test
    void execute_hasComponentOwnersFalseWithEmptyList_persists() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(false, List.of());

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.hasComponentOwners()).isFalse();
        assertThat(saved.componentOwners()).isEmpty();
    }

    @Test
    void execute_hasComponentOwnersFalseWithData_persistsWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(false, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null, null)));

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.hasComponentOwners()).isFalse();
        assertThat(saved.componentOwners()).hasSize(1);
    }

    @Test
    void execute_hasComponentOwnersNotSent_defaultsToFalse() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedRules();
        assertThat(saved.hasComponentOwners()).isFalse();
    }
}
