package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DocumentTemplateRef;
import com.getvaas.distribution.engine.domain.model.NotificationChannelsConfig;
import com.getvaas.distribution.engine.domain.model.NotificationTemplatesConfig;
import com.getvaas.distribution.engine.domain.model.NotificationsConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.DocumentTemplateRefRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationChannelsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationTemplatesRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateNotificationsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Configura la etapa Notifications — Events & Channels (VPR-9639) y Templates (VPR-9640). Ambas
 * sub-secciones son opcionales. {@code body} queda explícitamente sin modelar — bloqueado por una
 * pregunta sin responder sobre notifications-api.
 */
@Component
@RequiredArgsConstructor
public class UpdateNotificationsUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateNotificationsRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var channels = buildChannelsConfig(request.channels());
        var templates = buildTemplatesConfig(request.templates());
        var notifications = new NotificationsConfig(channels, templates);

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                existing.config().readinessChecks(),
                notifications
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
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
