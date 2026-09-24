package com.getvaas.distribution.engine.domain.model;

public record NotificationTemplate(
        Long id,
        String type,
        Integer channelId
) {}
