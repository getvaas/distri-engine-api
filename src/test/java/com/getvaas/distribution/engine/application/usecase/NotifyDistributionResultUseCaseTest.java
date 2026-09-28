package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DistributionExecutionResult;
import com.getvaas.distribution.engine.domain.model.NotificationChannelsConfig;
import com.getvaas.distribution.engine.domain.model.NotificationTemplatesConfig;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotifyDistributionResultUseCaseTest {

    private static final Long COMPANY_ID = 3L;

    @Mock
    private NotificationProvider notificationProvider;

    private NotifyDistributionResultUseCase useCase() {
        return new NotifyDistributionResultUseCase(notificationProvider);
    }

    private DistributionExecutionResult result() {
        var assignments = List.of(new Assignment("Owner Co", 61L, "Owner Co", new BigDecimal("100.00")));
        return new DistributionExecutionResult(null, new PartitionedPoolFunds(List.of(), List.of()), assignments, 427L);
    }

    private DistributionConfig configWith(NotificationsConfig notifications) {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, notifications, null);
        return new DistributionConfig("id-1", "Deal", COMPANY_ID, COMPANY_ID, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @Test
    void execute_notificationsConfigMissing_doesNothing() {
        useCase().execute(configWith(null), COMPANY_ID, result());

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_eventNotEnabled_doesNothing() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_FAILED)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);

        useCase().execute(configWith(notifications), COMPANY_ID, result());

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_noRecipients_doesNothing() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                new NotificationTemplatesConfig("Subject", List.of(), null),
                null);

        useCase().execute(configWith(notifications), COMPANY_ID, result());

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_eventEnabledWithRecipients_notifiesWithoutAttachments() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);

        useCase().execute(configWith(notifications), COMPANY_ID, result());

        verify(notificationProvider).notify(
                eq("DISTRIBUTION_SUCCEEDED"),
                eq(List.of("owner@example.com")),
                any(),
                eq(List.of()));
    }

    @Test
    void execute_providerThrows_doesNotPropagate() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);
        doThrow(new RuntimeException("notifications-api unavailable"))
                .when(notificationProvider).notify(any(), any(), any(), any());

        useCase().execute(configWith(notifications), COMPANY_ID, result());

        verify(notificationProvider).notify(eq("DISTRIBUTION_SUCCEEDED"), any(), any(), any());
    }
}
