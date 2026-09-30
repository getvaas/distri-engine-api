package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AssignmentEntity;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveDraftDistributionUseCaseTest {

    private static final Long COMPANY_ID = 3L;
    private static final Long DISTRIBUTION_ID = 99L;

    @Mock
    private MasterServicerDistributionJPARepository distributionRepository;
    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    @Mock
    private NotifyDistributionResultUseCase notifyDistributionResultUseCase;
    @Mock
    private NotifyTransferInstructionUseCase notifyTransferInstructionUseCase;

    private ApproveDraftDistributionUseCase useCase() {
        return new ApproveDraftDistributionUseCase(distributionRepository, resolveActiveDistributionConfigUseCase,
                notifyDistributionResultUseCase, notifyTransferInstructionUseCase);
    }

    private DistributionConfig activeConfig() {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", COMPANY_ID, 7L, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private MasterServicerDistributionEntity draftDistribution() {
        return MasterServicerDistributionEntity.builder()
                .id(DISTRIBUTION_ID)
                .status("draft")
                .assignments(List.of(AssignmentEntity.builder().accountId(61L).build()))
                .build();
    }

    @Test
    void execute_draftDistribution_approvesAndNotifies() {
        var distribution = draftDistribution();
        when(distributionRepository.findById(DISTRIBUTION_ID)).thenReturn(Optional.of(distribution));
        when(distributionRepository.save(distribution)).thenReturn(distribution);
        when(resolveActiveDistributionConfigUseCase.execute(COMPANY_ID)).thenReturn(activeConfig());

        useCase().execute(DISTRIBUTION_ID, COMPANY_ID);

        var captor = ArgumentCaptor.forClass(MasterServicerDistributionEntity.class);
        verify(distributionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("approved");
        verify(notifyDistributionResultUseCase).execute(any(), eq(COMPANY_ID), eq(DISTRIBUTION_ID), eq(1));
        verify(notifyTransferInstructionUseCase).execute(any(), eq(COMPANY_ID), eq(DISTRIBUTION_ID),
                eq(NotificationEvent.TRANSFER_INSTRUCTION_READY));
    }

    @Test
    void execute_distributionNotFound_throwsAndNeverNotifies() {
        when(distributionRepository.findById(DISTRIBUTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(DISTRIBUTION_ID, COMPANY_ID))
                .isInstanceOf(MasterServicerDistributionNotFoundException.class);

        verify(distributionRepository, never()).save(any());
        verify(notifyDistributionResultUseCase, never()).execute(any(), any(), any(), anyInt());
        verify(notifyTransferInstructionUseCase, never()).execute(any(), any(), any(), any());
    }

    @Test
    void execute_distributionNotInDraftStatus_throwsAndNeverNotifies() {
        var alreadyApproved = MasterServicerDistributionEntity.builder()
                .id(DISTRIBUTION_ID).status("approved").assignments(List.of()).build();
        when(distributionRepository.findById(DISTRIBUTION_ID)).thenReturn(Optional.of(alreadyApproved));

        assertThatThrownBy(() -> useCase().execute(DISTRIBUTION_ID, COMPANY_ID))
                .isInstanceOf(DistributionNotInDraftStatusException.class);

        verify(distributionRepository, never()).save(any());
        verify(notifyDistributionResultUseCase, never()).execute(any(), any(), any(), anyInt());
        verify(notifyTransferInstructionUseCase, never()).execute(any(), any(), any(), any());
    }
}
