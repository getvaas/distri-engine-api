package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.NotificationChannelsConfig;
import com.getvaas.distribution.engine.domain.model.NotificationTemplatesConfig;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.domain.model.TransferInstructionAssignment;
import com.getvaas.distribution.engine.domain.model.TransferInstructionsConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotifyTransferInstructionUseCaseTest {

    private static final Long COMPANY_ID = 3L;
    private static final Long DISTRIBUTION_ID = 427L;

    @Mock
    private NotificationProvider notificationProvider;

    private NotifyTransferInstructionUseCase useCase() {
        return new NotifyTransferInstructionUseCase(notificationProvider);
    }

    private DistributionConfig configWith(TransferInstructionsConfig transferInstructions,
                                           NotificationsConfig notifications) {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, notifications, transferInstructions, null);
        return new DistributionConfig("id-1", "Deal", COMPANY_ID, COMPANY_ID, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private TransferInstructionsConfig transferInstructionsWithOneAssignment() {
        return new TransferInstructionsConfig(List.of(new TransferInstructionAssignment("SOMOS_LENDER", "metadata.amount")));
    }

    private NotificationsConfig notificationsEnabledFor(NotificationEvent event) {
        return new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(event)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);
    }

    @Test
    void execute_transferInstructionsMissing_doesNothing() {
        useCase().execute(configWith(null, notificationsEnabledFor(NotificationEvent.TRANSFER_INSTRUCTION_READY)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_transferInstructionsEmpty_doesNothing() {
        var transferInstructions = new TransferInstructionsConfig(List.of());

        useCase().execute(configWith(transferInstructions, notificationsEnabledFor(NotificationEvent.TRANSFER_INSTRUCTION_READY)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_notificationsConfigMissing_doesNothing() {
        useCase().execute(configWith(transferInstructionsWithOneAssignment(), null),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_eventNotEnabled_doesNothing() {
        useCase().execute(configWith(transferInstructionsWithOneAssignment(),
                        notificationsEnabledFor(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_realEventEnabled_notifiesWithRealEvent() {
        useCase().execute(configWith(transferInstructionsWithOneAssignment(),
                        notificationsEnabledFor(NotificationEvent.TRANSFER_INSTRUCTION_READY)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verify(notificationProvider).notify(
                eq("TRANSFER_INSTRUCTION_READY"),
                eq(List.of("owner@example.com")),
                any(),
                eq(List.of()));
    }

    @Test
    void execute_draftEventEnabled_notifiesWithDraftEvent() {
        useCase().execute(configWith(transferInstructionsWithOneAssignment(),
                        notificationsEnabledFor(NotificationEvent.TRANSFER_INSTRUCTION_DRAFT_READY)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_DRAFT_READY);

        verify(notificationProvider).notify(
                eq("TRANSFER_INSTRUCTION_DRAFT_READY"),
                eq(List.of("owner@example.com")),
                any(),
                eq(List.of()));
    }

    @Test
    void execute_providerThrows_doesNotPropagate() {
        doThrow(new RuntimeException("notifications-api unavailable"))
                .when(notificationProvider).notify(any(), any(), any(), any());

        useCase().execute(configWith(transferInstructionsWithOneAssignment(),
                        notificationsEnabledFor(NotificationEvent.TRANSFER_INSTRUCTION_READY)),
                COMPANY_ID, DISTRIBUTION_ID, NotificationEvent.TRANSFER_INSTRUCTION_READY);

        verify(notificationProvider).notify(eq("TRANSFER_INSTRUCTION_READY"), any(), any(), any());
    }
}
