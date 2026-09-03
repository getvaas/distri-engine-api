package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.infrastructure.web.dto.DocumentTemplateRefRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationChannelsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationTemplatesRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateSftpDeliveryRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationsConfigBuilderTest {

    private final NotificationsConfigBuilder builder = new NotificationsConfigBuilder();

    @Test
    void build_channelsAndEvents_persistAsIs() {
        var request = new UpdateNotificationsRequest(
                new UpdateNotificationChannelsRequest(
                        List.of(NotificationChannel.SLACK, NotificationChannel.EMAIL),
                        List.of(NotificationEvent.DISTRIBUTION_SUCCEEDED, NotificationEvent.DISTRIBUTION_FAILED)),
                null, null);

        NotificationsConfig saved = builder.build(request);

        assertThat(saved.channels().channels()).containsExactly(NotificationChannel.SLACK, NotificationChannel.EMAIL);
        assertThat(saved.channels().enabledEvents()).containsExactly(
                NotificationEvent.DISTRIBUTION_SUCCEEDED, NotificationEvent.DISTRIBUTION_FAILED);
    }

    @Test
    void build_templatesWithValidDocuments_persistAsIs() {
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(
                        "Distribution SOMOS - {{date}}",
                        List.of("ops@getvaas.com", "lender@architect.com"),
                        List.of(new DocumentTemplateRefRequest(
                                "SOMOS_TRANSFER_INSTRUCTION_V2", "transfer_instruction_v2.docx", "Transfer instruction", "PDF"))),
                null);

        NotificationsConfig saved = builder.build(request);

        assertThat(saved.templates().subject()).isEqualTo("Distribution SOMOS - {{date}}");
        assertThat(saved.templates().recipients()).containsExactly("ops@getvaas.com", "lender@architect.com");
        assertThat(saved.templates().documents()).hasSize(1);
        assertThat(saved.templates().documents().get(0).name()).isEqualTo("SOMOS_TRANSFER_INSTRUCTION_V2");
        assertThat(saved.templates().documents().get(0).format()).isEqualTo("PDF");
    }

    @Test
    void build_documentWithoutName_throwsInvalidDistributionConfigException() {
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(null, null,
                        List.of(new DocumentTemplateRefRequest(null, "file.docx", null, null))),
                null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_documentWithoutFileName_throwsInvalidDistributionConfigException() {
        var request = new UpdateNotificationsRequest(null,
                new UpdateNotificationTemplatesRequest(null, null,
                        List.of(new DocumentTemplateRefRequest("NAME", null, null, null))),
                null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_channelsNotSent_persistsNullWithoutError() {
        NotificationsConfig saved = builder.build(new UpdateNotificationsRequest(null, null, null));

        assertThat(saved.channels()).isNull();
    }

    @Test
    void build_templatesNotSent_persistsNullWithoutError() {
        NotificationsConfig saved = builder.build(new UpdateNotificationsRequest(null, null, null));

        assertThat(saved.templates()).isNull();
    }

    // ===== SFTP Delivery (VPR-9721) =====

    @Test
    void build_sftpDeliveryEnabledWithRequiredFields_persistsAsIs() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "5da3f818-b8ef-4db5-972f-6422f18e72f4",
                        "/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/", "{prefix}{ddMMyyyy}{HHmmss}", null));

        NotificationsConfig saved = builder.build(request);

        assertThat(saved.sftpDelivery().enabled()).isTrue();
        assertThat(saved.sftpDelivery().credentialKey()).isEqualTo("5da3f818-b8ef-4db5-972f-6422f18e72f4");
        assertThat(saved.sftpDelivery().remotePathTemplate()).isEqualTo("/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/");
        assertThat(saved.sftpDelivery().fileNameTemplate()).isEqualTo("{prefix}{ddMMyyyy}{HHmmss}");
        assertThat(saved.sftpDelivery().encryptionKeyRef()).isNull();
    }

    @Test
    void build_sftpDeliveryEnabledWithEncryptionKeyRef_persistsAsIs() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", "/path/", "file.txt", "liquitech-pgp-key"));

        NotificationsConfig saved = builder.build(request);

        assertThat(saved.sftpDelivery().encryptionKeyRef()).isEqualTo("liquitech-pgp-key");
    }

    @Test
    void build_sftpDeliveryEnabledWithoutCredentialKey_throwsInvalidDistributionConfigException() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, null, "/path/", "file.txt", null));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_sftpDeliveryEnabledWithoutRemotePathTemplate_throwsInvalidDistributionConfigException() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", null, "file.txt", null));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_sftpDeliveryEnabledWithoutFileNameTemplate_throwsInvalidDistributionConfigException() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(true, "cred-key", "/path/", null, null));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_sftpDeliveryDisabledWithoutRequiredFields_doesNotThrow() {
        var request = new UpdateNotificationsRequest(null, null,
                new UpdateSftpDeliveryRequest(false, null, null, null, null));

        NotificationsConfig saved = builder.build(request);

        assertThat(saved.sftpDelivery().enabled()).isFalse();
        assertThat(saved.sftpDelivery().credentialKey()).isNull();
    }

    @Test
    void build_sftpDeliveryNotSent_persistsNullWithoutError() {
        NotificationsConfig saved = builder.build(new UpdateNotificationsRequest(null, null, null));

        assertThat(saved.sftpDelivery()).isNull();
    }
}
