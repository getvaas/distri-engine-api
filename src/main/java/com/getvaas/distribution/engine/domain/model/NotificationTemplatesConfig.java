package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Notifications — Templates (VPR-9640). {@code subject} y {@code recipients} son una sola lista
 * plana (mockup real no muestra dos grupos de audiencia — caso de Inklusiva con 2 audiencias
 * distintas queda como gap conocido, sin resolver). {@code documents} son los adjuntos generados
 * por la documents API. No incluye {@code body} — bloqueado por una pregunta sin responder sobre
 * notifications-api (¿editable sin deploy, y por quién?), no es una decisión de negocio nuestra.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationTemplatesConfig(
        String subject,
        List<String> recipients,
        List<DocumentTemplateRef> documents
) {}
