package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Etapa 8 — Notifications. {@code channels} (VPR-9639) y {@code templates} (VPR-9640) ya están
 * tipados y son ambos opcionales — un deal puede no tener ninguno configurado todavía.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationsConfig(
        NotificationChannelsConfig channels,
        NotificationTemplatesConfig templates
) {}
