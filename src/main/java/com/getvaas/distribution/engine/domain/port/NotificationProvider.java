package com.getvaas.distribution.engine.domain.port;

import com.getvaas.distribution.engine.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Map;

/**
 * Puerto hacia el microservicio externo {@code notifications-api} (VPR-9671). {@code type}
 * corresponde a un {@link com.getvaas.distribution.engine.domain.model.enums.NotificationEvent}
 * (por nombre); {@code attachmentsUrls} queda modelado desde el arranque porque es parte del
 * contrato real, aunque esta primera iteración siempre lo manda vacío — adjuntar el reporte de
 * VPR-9670 es una mejora posterior, no de esta historia.
 * <p>
 * {@code getTemplates}/{@code createTemplate} replican {@code createDefaultNotificationTemplate}
 * del sistema real ({@code CreateDistributionConfig.kt}) — verificado contra ese código: la query
 * real solo manda {@code type}+{@code page}+{@code limit} (sin {@code channel_id} como query
 * param), la respuesta viene envuelta en {@code {"data": [...]}}, y el filtro por
 * {@code channelId} se hace del lado del cliente.
 */
public interface NotificationProvider {

    void notify(String type, List<String> recipients, Map<String, String> context, List<String> attachmentsUrls);

    List<NotificationTemplate> getTemplates(String type, Integer channelId);

    Long createTemplate(String text, String type, Integer channelId);
}
