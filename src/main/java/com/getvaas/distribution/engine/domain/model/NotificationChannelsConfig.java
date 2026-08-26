package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.NotificationChannel;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;

import java.util.List;

/**
 * Notifications — Events & Channels (VPR-9639). Lista plana de canales y eventos habilitados,
 * sin mapeo evento→canal individual — coincide con el mockup real
 * ({@code docs/distribution-engine-onboarding.html} STEP 7): un solo selector de canales
 * (mezclando internos como Slack/ROAM con el canal cliente Email) y una lista de toggles de
 * eventos independiente. SFTP (E7b) no aparece en el mockup — sin lugar todavía en esta config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationChannelsConfig(
        List<NotificationChannel> channels,
        List<NotificationEvent> enabledEvents
) {}
