package com.getvaas.distribution.engine.domain.port;

import java.util.List;
import java.util.Map;

/**
 * Puerto hacia el microservicio externo {@code notifications-api} (VPR-9671). {@code type}
 * corresponde a un {@link com.getvaas.distribution.engine.domain.model.enums.NotificationEvent}
 * (por nombre); {@code attachmentsUrls} queda modelado desde el arranque porque es parte del
 * contrato real, aunque esta primera iteración siempre lo manda vacío — adjuntar el reporte de
 * VPR-9670 es una mejora posterior, no de esta historia.
 */
public interface NotificationProvider {

    void notify(String type, List<String> recipients, Map<String, String> context, List<String> attachmentsUrls);
}
