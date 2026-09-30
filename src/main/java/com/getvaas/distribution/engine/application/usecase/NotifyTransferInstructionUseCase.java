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
 * Paso 10e del pipeline de ejecución — avisa que la instrucción de transferencia está lista
 * (Transfer Instructions, VPR-9713/9714, hasta ahora solo config). Mismo patrón que
 * {@link NotifyDistributionResultUseCase} (no adjunta todavía el reporte real, VPR-9670, mejora
 * posterior).
 * <p>
 * El evento a disparar lo decide el caller según el status de la distribución (VPR-9876):
 * {@code TRANSFER_INSTRUCTION_READY} es la instrucción real (dispara movimiento de fondos del lado
 * del servicer) — solo corresponde cuando la distribución quedó {@code APPROVED} (de entrada, o al
 * aprobar un draft vía {@link ApproveDraftDistributionUseCase}). {@code TRANSFER_INSTRUCTION_DRAFT_READY}
 * es el mismo reporte pero para una distribución que quedó {@code DRAFT} — mismos
 * destinatarios/canales ya configurados en Notifications, pero un evento distinto para que el
 * template/asunto le deje claro al aprobador que es un borrador para revisión, no la instrucción
 * real. Si el deal no tiene {@code transferInstructions} configurado (sin ningún owner asignado a
 * plantilla), o {@code notifications}, o no tiene el evento habilitado, o no tiene destinatarios,
 * no hace nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyTransferInstructionUseCase {

    private final NotificationProvider notificationProvider;

    public void execute(DistributionConfig config, Long companyId, Long distributionId, NotificationEvent event) {
        var transferInstructions = config.config().transferInstructions();
        if (transferInstructions == null || transferInstructions.assignments() == null
                || transferInstructions.assignments().isEmpty()) {
            return;
        }

        var notifications = config.config().notifications();
        if (notifications == null || notifications.channels() == null || notifications.templates() == null) {
            return;
        }

        var enabledEvents = notifications.channels().enabledEvents();
        if (enabledEvents == null || !enabledEvents.contains(event)) {
            return;
        }

        var recipients = notifications.templates().recipients();
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        try {
            notificationProvider.notify(event.name(), recipients,
                    buildContext(companyId, distributionId, transferInstructions.assignments().size(), event), List.of());
        } catch (Exception e) {
            log.warn("No se pudo notificar la instrucción de transferencia [companyId={}, distributionId={}, event={}]: {}",
                    companyId, distributionId, event, e.getMessage(), e);
        }
    }

    // "draft" no se persiste en ninguna tabla/entidad — se deriva en el momento del `event` que ya
    // decide el caller (RunDistributionUseCase/ApproveDraftDistributionUseCase), a partir del
    // status real de la distribución. Va en el contexto para que el template/consumer de
    // notifications-api lo tenga explícito, sin depender de parsear el nombre del evento.
    private Map<String, String> buildContext(Long companyId, Long distributionId, int transferAssignmentsCount,
                                              NotificationEvent event) {
        var context = new HashMap<String, String>();
        context.put("companyId", String.valueOf(companyId));
        context.put("distributionId", String.valueOf(distributionId));
        context.put("transferAssignmentsCount", String.valueOf(transferAssignmentsCount));
        context.put("draft", String.valueOf(event == NotificationEvent.TRANSFER_INSTRUCTION_DRAFT_READY));
        return context;
    }
}
