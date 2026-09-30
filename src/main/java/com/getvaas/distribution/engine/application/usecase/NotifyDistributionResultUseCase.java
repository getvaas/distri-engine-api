package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Primera iteración funcional de VPR-9671: avisa el resultado de una distribución exitosa a
 * {@code notifications-api}. No adjunta todavía el reporte de VPR-9670 ({@code attachmentsUrls}
 * siempre vacío) ni cubre el camino de fallo ({@code DISTRIBUTION_FAILED}) — quedan para una
 * mejora posterior. Si el deal no tiene {@code notifications} configurado, o no tiene el evento
 * habilitado, o no tiene destinatarios, no hace nada. Una falla de {@link NotificationProvider} se
 * loguea y nunca se propaga — la distribución ya quedó persistida antes de llegar acá.
 * <p>
 * Firma reducida a los datos mínimos que realmente usa (VPR-9876) — antes recibía el
 * {@code DistributionExecutionResult} completo de {@code RunDistributionUseCase}, pero
 * {@code ApproveDraftDistributionUseCase} también necesita notificar (al aprobar un draft) sin
 * tener un {@code DistributionExecutionResult} disponible (no hay readiness/funds al aprobar algo
 * ya persistido).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyDistributionResultUseCase {

    private static final NotificationEvent EVENT = NotificationEvent.DISTRIBUTION_SUCCEEDED;

    private final NotificationProvider notificationProvider;

    public void execute(DistributionConfig config, Long companyId, Long distributionId, int assignmentsCount) {
        var notifications = config.config().notifications();
        if (notifications == null || notifications.channels() == null || notifications.templates() == null) {
            log.info("Resultado de distribución no notificado [companyId={}, distributionId={}]: "
                    + "'notifications' no configurado (channels/templates)", companyId, distributionId);
            return;
        }

        var enabledEvents = notifications.channels().enabledEvents();
        if (enabledEvents == null || !enabledEvents.contains(EVENT)) {
            log.info("Resultado de distribución no notificado [companyId={}, distributionId={}]: "
                    + "{} no habilitado en notifications.channels.enabledEvents (tiene: {})",
                    companyId, distributionId, EVENT, enabledEvents);
            return;
        }

        var recipients = notifications.templates().recipients();
        if (recipients == null || recipients.isEmpty()) {
            log.info("Resultado de distribución no notificado [companyId={}, distributionId={}]: "
                    + "sin destinatarios en notifications.templates.recipients", companyId, distributionId);
            return;
        }

        log.info("Notificando resultado de distribución [companyId={}, distributionId={}, recipients={}]",
                companyId, distributionId, recipients);
        try {
            notificationProvider.notify(EVENT.name(), recipients, buildContext(companyId, distributionId, assignmentsCount), List.of());
            log.info("Resultado de distribución notificado OK [companyId={}, distributionId={}]", companyId, distributionId);
        } catch (Exception e) {
            log.warn("No se pudo notificar el resultado de la distribución [companyId={}, distributionId={}]: {}",
                    companyId, distributionId, e.getMessage(), e);
        }
    }

    private Map<String, String> buildContext(Long companyId, Long distributionId, int assignmentsCount) {
        var context = new HashMap<String, String>();
        context.put("companyId", String.valueOf(companyId));
        context.put("distributionId", String.valueOf(distributionId));
        context.put("assignmentsCount", String.valueOf(assignmentsCount));
        return context;
    }
}
