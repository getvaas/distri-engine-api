package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Etapa 8 — Notifications. {@code channels} (VPR-9639), {@code templates} (VPR-9640) y
 * {@code sftpDelivery} (VPR-9721) ya están tipados y son todos opcionales — un deal puede no
 * tener ninguno configurado todavía.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationsConfig(
        NotificationChannelsConfig channels,
        NotificationTemplatesConfig templates,
        SftpDeliveryConfig sftpDelivery
) {}
