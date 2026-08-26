package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;

import java.util.List;

public record UpdateNotificationChannelsRequest(
        List<NotificationChannel> channels,
        List<NotificationEvent> enabledEvents
) {}
