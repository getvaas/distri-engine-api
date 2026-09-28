package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.NotificationChannelsConfig;
import com.getvaas.distribution.engine.domain.model.NotificationTemplate;
import com.getvaas.distribution.engine.domain.model.NotificationTemplatesConfig;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnsureNotificationTemplateUseCaseTest {

    private static final Long COMPANY_ID = 999601L;
    private static final String EXPECTED_TYPE = "999601_DISTRIBUTION_SUCCEEDED";

    @Mock
    private NotificationProvider notificationProvider;

    private EnsureNotificationTemplateUseCase useCase() {
        return new EnsureNotificationTemplateUseCase(notificationProvider);
    }

    private DistributionConfig configWith(NotificationsConfig notifications) {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, notifications, null);
        return new DistributionConfig("id-1", "Deal", COMPANY_ID, COMPANY_ID, DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private NotificationsConfig notificationsConfig() {
        return new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);
    }

    @Test
    void execute_nullConfig_doesNothing() {
        useCase().execute(null);

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_notificationsMissing_doesNothing() {
        useCase().execute(configWith(null));

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_eventNotEnabled_doesNothing() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_FAILED)),
                new NotificationTemplatesConfig("Subject", List.of("owner@example.com"), null),
                null);

        useCase().execute(configWith(notifications));

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_noRecipients_doesNothing() {
        var notifications = new NotificationsConfig(
                new NotificationChannelsConfig(List.of(NotificationChannel.EMAIL), List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED)),
                new NotificationTemplatesConfig("Subject", List.of(), null),
                null);

        useCase().execute(configWith(notifications));

        verifyNoInteractions(notificationProvider);
    }

    @Test
    void execute_templateAlreadyExists_doesNotCreateOne() {
        when(notificationProvider.getTemplates(EXPECTED_TYPE, 3))
                .thenReturn(List.of(new NotificationTemplate(42L, EXPECTED_TYPE, 3)));

        useCase().execute(configWith(notificationsConfig()));

        verify(notificationProvider, never()).createTemplate(any(), any(), any());
    }

    @Test
    void execute_templateMissing_createsItWithChannelId3() {
        when(notificationProvider.getTemplates(EXPECTED_TYPE, 3)).thenReturn(List.of());

        useCase().execute(configWith(notificationsConfig()));

        verify(notificationProvider).createTemplate(any(), eq(EXPECTED_TYPE), eq(3));
    }

    @Test
    void execute_providerThrows_doesNotPropagate() {
        when(notificationProvider.getTemplates(EXPECTED_TYPE, 3))
                .thenThrow(new RuntimeException("notifications-api unavailable"));

        useCase().execute(configWith(notificationsConfig()));

        verify(notificationProvider, never()).createTemplate(any(), any(), any());
    }

    @Test
    void execute_createTemplateThrows_doesNotPropagate() {
        when(notificationProvider.getTemplates(EXPECTED_TYPE, 3)).thenReturn(List.of());
        doThrow(new RuntimeException("notifications-api unavailable"))
                .when(notificationProvider).createTemplate(any(), any(), any());

        useCase().execute(configWith(notificationsConfig()));

        verify(notificationProvider).createTemplate(any(), eq(EXPECTED_TYPE), eq(3));
    }
}
