package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipMismatchStrategy;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipCrossValidationRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipSourceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOwnershipUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateOwnershipUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

    private DistributionConfig existingDomain() {
        return new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, EMPTY_PAYLOAD,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private void mockExisting() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain());
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    private OwnershipConfig captureSavedOwnership() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue().ownership();
    }

    @Test
    void execute_ownershipApiSourceWithField_persistsAsIs() {
        mockExisting();
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.OWNERSHIP_API, "contract_id", null), null);

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.source().sourceType()).isEqualTo(OwnershipSourceType.OWNERSHIP_API);
        assertThat(saved.source().field()).isEqualTo("contract_id");
        assertThat(saved.source().defaultOwner()).isNull();
    }

    @Test
    void execute_paymentTapeFieldSourceWithJsonSubPath_persistsAsIs() {
        mockExisting();
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "extra_data.aux_var_3", "UNKNOWN_OWNER"), null);

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.source().sourceType()).isEqualTo(OwnershipSourceType.PAYMENT_TAPE_FIELD);
        assertThat(saved.source().field()).isEqualTo("extra_data.aux_var_3");
        assertThat(saved.source().defaultOwner()).isEqualTo("UNKNOWN_OWNER");
    }

    @Test
    void execute_sourceWithoutField_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.OWNERSHIP_API, null, null), null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sourceWithoutSourceType_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(null, "contract_id", null), null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sourceNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateOwnershipRequest(null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.source()).isNull();
    }

    private void assertCrossValidationPersists(OwnershipMismatchStrategy strategy) {
        mockExisting();
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(true, strategy));

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.crossValidation().enabled()).isTrue();
        assertThat(saved.crossValidation().mismatchStrategy()).isEqualTo(strategy);
    }

    @Test
    void execute_crossValidationEnabledWithApiWins_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.API_WINS);
    }

    @Test
    void execute_crossValidationEnabledWithTapeWins_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.TAPE_WINS);
    }

    @Test
    void execute_crossValidationEnabledWithBlockPayment_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.BLOCK_PAYMENT);
    }

    @Test
    void execute_crossValidationEnabledWithBlockDistribution_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.BLOCK_DISTRIBUTION);
    }

    @Test
    void execute_crossValidationEnabledWithoutMismatchStrategy_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(true, null));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_crossValidationDisabledWithMismatchStrategySent_ignoresItAndPersistsNull() {
        mockExisting();
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(false, OwnershipMismatchStrategy.API_WINS));

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.crossValidation().enabled()).isFalse();
        assertThat(saved.crossValidation().mismatchStrategy()).isNull();
    }

    @Test
    void execute_crossValidationNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateOwnershipRequest(null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedOwnership();
        assertThat(saved.crossValidation()).isNull();
    }

    @Test
    void execute_preservesRestOfPayload() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, existingPayload, LocalDateTime.now(), LocalDateTime.now(), null, null));
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", new UpdateOwnershipRequest(null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }
}
