package com.getvaas.distribution.engine.infrastructure.web.dto;

public record UpdateNotificationsRequest(
        UpdateNotificationChannelsRequest channels,
        UpdateNotificationTemplatesRequest templates
) {}
