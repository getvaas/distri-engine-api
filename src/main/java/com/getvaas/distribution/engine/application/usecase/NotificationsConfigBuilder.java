package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DocumentTemplateRef;
import com.getvaas.distribution.engine.domain.model.NotificationChannelsConfig;
import com.getvaas.distribution.engine.domain.model.NotificationTemplatesConfig;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.domain.model.SftpDeliveryConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.DocumentTemplateRefRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationChannelsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationTemplatesRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateSftpDeliveryRequest;
import org.springframework.stereotype.Component;

/**
 * Construye la etapa Notifications — Events & Channels (VPR-9639), Templates (VPR-9640) y SFTP
 * delivery (VPR-9721). Todas las sub-secciones son opcionales. {@code body} queda explícitamente
 * sin modelar — bloqueado por una pregunta sin responder sobre notifications-api.
 */
@Component
public class NotificationsConfigBuilder {

    public NotificationsConfig build(UpdateNotificationsRequest request) {
        var channels = buildChannelsConfig(request.channels());
        var templates = buildTemplatesConfig(request.templates());
        var sftpDelivery = buildSftpDeliveryConfig(request.sftpDelivery());
        return new NotificationsConfig(channels, templates, sftpDelivery);
    }

    private NotificationChannelsConfig buildChannelsConfig(UpdateNotificationChannelsRequest request) {
        if (request == null) {
            return null;
        }
        return new NotificationChannelsConfig(request.channels(), request.enabledEvents());
    }

    private NotificationTemplatesConfig buildTemplatesConfig(UpdateNotificationTemplatesRequest request) {
        if (request == null) {
            return null;
        }

        var documents = request.documents() == null
                ? null
                : request.documents().stream().map(this::buildDocumentTemplateRef).toList();

        return new NotificationTemplatesConfig(request.subject(), request.recipients(), documents);
    }

    private SftpDeliveryConfig buildSftpDeliveryConfig(UpdateSftpDeliveryRequest request) {
        if (request == null) {
            return null;
        }

        var enabled = Boolean.TRUE.equals(request.enabled());
        if (enabled) {
            if (request.credentialKey() == null || request.credentialKey().isBlank()) {
                throw new InvalidDistributionConfigException("'sftpDelivery' con enabled=true requiere 'credentialKey'");
            }
            if (request.remotePathTemplate() == null || request.remotePathTemplate().isBlank()) {
                throw new InvalidDistributionConfigException("'sftpDelivery' con enabled=true requiere 'remotePathTemplate'");
            }
            if (request.fileNameTemplate() == null || request.fileNameTemplate().isBlank()) {
                throw new InvalidDistributionConfigException("'sftpDelivery' con enabled=true requiere 'fileNameTemplate'");
            }
        }

        return new SftpDeliveryConfig(
                enabled, request.credentialKey(), request.remotePathTemplate(), request.fileNameTemplate(), request.encryptionKeyRef());
    }

    private DocumentTemplateRef buildDocumentTemplateRef(DocumentTemplateRefRequest documentRequest) {
        if (documentRequest.name() == null || documentRequest.name().isBlank()) {
            throw new InvalidDistributionConfigException("cada documento adjunto requiere 'name'");
        }
        if (documentRequest.fileName() == null || documentRequest.fileName().isBlank()) {
            throw new InvalidDistributionConfigException("cada documento adjunto requiere 'fileName'");
        }

        return new DocumentTemplateRef(
                documentRequest.name(), documentRequest.fileName(), documentRequest.description(), documentRequest.format());
    }
}
