package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
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
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null),
                new ComponentOwnerRuleRequest(PaymentComponent.INTEREST, "funder", null),
                new ComponentOwnerRuleRequest(PaymentComponent.LATE_FEE, "servicer", "late fees go to servicer"),
                new ComponentOwnerRuleRequest(PaymentComponent.GUARANTEE, "guarantee_fund", null)));

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
                new ComponentOwnerRuleRequest(null, "funder", null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_ruleWithoutOwner_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, null, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_duplicateComponent_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null),
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "servicer", null)));

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
    void execute_hasComponentOwnersTrue_persists() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateDistributionRulesRequest(true, List.of(
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null)));

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
                new ComponentOwnerRuleRequest(PaymentComponent.PRINCIPAL, "funder", null)));

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
