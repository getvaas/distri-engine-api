package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.DocumentTemplateRefRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationChannelsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationTemplatesRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationsRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateNotificationsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateNotificationsUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null);

    private void mockExisting() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        var existing = new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, EMPTY_PAYLOAD,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existing);
        when(repository.save(entity)).thenReturn(entity);
    }

    private NotificationsConfig captureSavedNotifications() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue().notifications();
    }

    @Test
    void execute_channelsAndEvents_persistAsIs() {
        mockExisting();
        var request = new UpdateNotificationsRequest(
                new UpdateNotificationChannelsRequest(
                        List.of(NotificationChannel.SLACK, NotificationChannel.EMAIL),
                        List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED, NotificationEvent.DISTRIBUTION_FAILED)),
                null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.channels().channels()).containsExactly(NotificationChannel.SLACK, NotificationChannel.EMAIL);
        assertThat(saved.channels().enabledEvents()).containsExactly(
                NotificationEvent.DISTRIBUTION_SUCCEEDED, NotificationEvent.DISTRIBUTION_FAILED);
    }

    @Test
    void execute_templatesWithValidDocuments_persistAsIs() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(
                        "Distribution SOMOS - {{date}}",
                        List.of("ops@getvaas.com", "lender@architect.com"),
                        List.of(new DocumentTemplateRefRequest(
                                "SOMOS_TRANSFER_INSTRUCTION_V2", "transfer_instruction_v2.docx", "Transfer instruction", "PDF"))));

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.templates().subject()).isEqualTo("Distribution SOMOS - {{date}}");
        assertThat(saved.templates().recipients()).containsExactly("ops@getvaas.com", "lender@architect.com");
        assertThat(saved.templates().documents()).hasSize(1);
        assertThat(saved.templates().documents().get(0).name()).isEqualTo("SOMOS_TRANSFER_INSTRUCTION_V2");
        assertThat(saved.templates().documents().get(0).format()).isEqualTo("PDF");
    }

    @Test
    void execute_documentWithoutName_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(null, null,
                        List.of(new DocumentTemplateRefRequest(null, "file.docx", null, null))));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_documentWithoutFileName_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(null, null,
                        List.of(new DocumentTemplateRefRequest("NAME", null, null, null))));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_channelsNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.channels()).isNull();
    }

    @Test
    void execute_templatesNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.templates()).isNull();
    }

    @Test
    void execute_preservesRestOfPayload() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, existingPayload, LocalDateTime.now(), LocalDateTime.now(), null, null));
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", new UpdateNotificationsRequest(null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }
}
