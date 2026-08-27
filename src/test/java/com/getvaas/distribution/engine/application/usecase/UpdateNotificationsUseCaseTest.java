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
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateSftpDeliveryRequest;
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
class UpdateNotificationsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateNotificationsUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

    private void mockExisting() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        var existing = new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, EMPTY_PAYLOAD,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existing);
        lenient().when(repository.save(entity)).thenReturn(entity);
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
                null, null);

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
                                "SOMOS_TRANSFER_INSTRUCTION_V2", "transfer_instruction_v2.docx", "Transfer instruction", "PDF"))),
                null);

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
                        List.of(new DocumentTemplateRefRequest(null, "file.docx", null, null))),
                null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_documentWithoutFileName_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(null, null,
                        List.of(new DocumentTemplateRefRequest("NAME", null, null, null))),
                null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_channelsNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.channels()).isNull();
    }

    @Test
    void execute_templatesNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.templates()).isNull();
    }

    @Test
    void execute_preservesRestOfPayload() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, existingPayload, LocalDateTime.now(), LocalDateTime.now(), null, null));
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", new UpdateNotificationsRequest(null, null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }

    // ===== SFTP Delivery (VPR-9721) =====

    @Test
    void execute_sftpDeliveryEnabledWithRequiredFields_persistsAsIs() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "5da3f818-b8ef-4db5-972f-6422f18e72f4",
                        "/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/", "{prefix}{ddMMyyyy}{HHmmss}", null));

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.sftpDelivery().enabled()).isTrue();
        assertThat(saved.sftpDelivery().credentialKey()).isEqualTo("5da3f818-b8ef-4db5-972f-6422f18e72f4");
        assertThat(saved.sftpDelivery().remotePathTemplate()).isEqualTo("/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/");
        assertThat(saved.sftpDelivery().fileNameTemplate()).isEqualTo("{prefix}{ddMMyyyy}{HHmmss}");
        assertThat(saved.sftpDelivery().encryptionKeyRef()).isNull();
    }

    @Test
    void execute_sftpDeliveryEnabledWithEncryptionKeyRef_persistsAsIs() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", "/path/", "file.txt", "liquitech-pgp-key"));

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.sftpDelivery().encryptionKeyRef()).isEqualTo("liquitech-pgp-key");
    }

    @Test
    void execute_sftpDeliveryEnabledWithoutCredentialKey_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, null, "/path/", "file.txt", null));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sftpDeliveryEnabledWithoutRemotePathTemplate_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", null, "file.txt", null));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sftpDeliveryEnabledWithoutFileNameTemplate_throwsInvalidDistributionConfigException() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", "/path/", null, null));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_sftpDeliveryDisabledWithoutRequiredFields_doesNotThrow() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(false, null, null, null, null));

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.sftpDelivery().enabled()).isFalse();
        assertThat(saved.sftpDelivery().credentialKey()).isNull();
    }

    @Test
    void execute_sftpDeliveryNotSent_persistsNullWithoutError() {
        mockExisting();
        var request = new UpdateNotificationsRequest(null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedNotifications();
        assertThat(saved.sftpDelivery()).isNull();
    }
}
