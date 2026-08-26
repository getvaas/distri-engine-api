package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record UpdateNotificationTemplatesRequest(
        String subject,
        List<String> recipients,
        List<DocumentTemplateRefRequest> documents
) {}
