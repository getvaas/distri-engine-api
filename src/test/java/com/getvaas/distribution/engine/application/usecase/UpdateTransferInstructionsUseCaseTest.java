package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.TransferInstructionAssignmentRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateTransferInstructionsRequest;
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
class UpdateTransferInstructionsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateTransferInstructionsUseCase useCase;

    private DistributionConfig existingDomain() {
        var ownership = new OwnershipConfig(null, null);
        var payload = new DistributionConfigPayload(
                "Colombia (COL)", "COP", null, null, null, null, ownership, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.DRAFT, payload, LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private void mockExisting() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain());
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    private DistributionConfigPayload captureSavedPayload() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue();
    }

    @Test
    void execute_validAssignments_persistsList() {
        mockExisting();

        useCase.execute("id-1", new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("FINAMCO", "metadata.reserve"))));

        var config = captureSavedPayload().transferInstructions();
        assertThat(config.assignments()).extracting("ownerTemplateCode", "namespace").containsExactly(
                org.assertj.core.groups.Tuple.tuple("PAYJOY", "metadata.amount"),
                org.assertj.core.groups.Tuple.tuple("FINAMCO", "metadata.reserve"));
    }

    @Test
    void execute_missingNamespace_throwsInvalidDistributionConfigException() {
        mockExisting();

        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_blankNamespace_throwsInvalidDistributionConfigException() {
        mockExisting();

        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "   ")));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_duplicateOwnerTemplateCode_throwsInvalidDistributionConfigException() {
        mockExisting();

        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.reserve")));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_repeatedNamespaceAcrossAssignments_persistsWithoutConflict() {
        mockExisting();

        useCase.execute("id-1", new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("FINAMCO", "metadata.amount"))));

        var config = captureSavedPayload().transferInstructions();
        assertThat(config.assignments()).hasSize(2);
        assertThat(config.assignments()).allSatisfy(a -> assertThat(a.namespace()).isEqualTo("metadata.amount"));
    }

    @Test
    void execute_emptyList_persistsEmptyList() {
        mockExisting();

        useCase.execute("id-1", new UpdateTransferInstructionsRequest(List.of()));

        var config = captureSavedPayload().transferInstructions();
        assertThat(config.assignments()).isEmpty();
    }

    @Test
    void execute_nullList_persistsEmptyList() {
        mockExisting();

        useCase.execute("id-1", new UpdateTransferInstructionsRequest(null));

        var config = captureSavedPayload().transferInstructions();
        assertThat(config.assignments()).isEmpty();
    }

    @Test
    void execute_updatesTransferInstructions_preservesRestOfPayload() {
        mockExisting();

        useCase.execute("id-1", new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"))));

        var payload = captureSavedPayload();
        assertThat(payload.country()).isEqualTo("Colombia (COL)");
        assertThat(payload.currency()).isEqualTo("COP");
        assertThat(payload.ownership()).isNotNull();
    }
}
