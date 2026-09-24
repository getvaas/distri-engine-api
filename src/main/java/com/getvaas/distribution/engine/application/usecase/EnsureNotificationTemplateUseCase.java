package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.NotificationEvent;
import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Réplica de {@code createDefaultNotificationTemplate} (sistema real, {@code CreateDistributionConfig.kt:52-76}):
 * al crear una {@code DistributionConfig} con notificaciones configuradas para
 * {@code DISTRIBUTION_SUCCEEDED}, se asegura de que exista un template en {@code notifications-api}
 * para ese type+channelId — si no existe, lo crea con el HTML default real — para que
 * {@link NotifyDistributionResultUseCase} no falle más adelante por falta de template. Igual que el
 * sistema real, corre una sola vez, en la creación de la config (no en cada corrida ni en un
 * update posterior).
 * <p>
 * {@code channelId=3} y el HTML default están copiados literal del sistema real
 * ({@code TRANSFER_INSTRUCTION_CHANNEL_ID}/{@code DEFAULT_HTML}, confirmados con el usuario) — acá
 * el {@code type} usa {@code companyId} en vez de un código de borrower (no tenemos ese concepto
 * expuesto en esta config), formato {@code "<companyId>_DISTRIBUTION_SUCCEEDED"}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureNotificationTemplateUseCase {

    private static final Integer NOTIFICATION_CHANNEL_ID = 3;

    private static final String DEFAULT_HTML = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Correo HTML</title>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f3f3f3; margin: 0; padding: 0; }
                    .container { width: 100%; max-width: 600px; margin: 20px auto; background-color: white; padding: 20px; box-sizing: border-box; border-radius: 8px; overflow: hidden; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; padding: 20px 0; }
                    .header img { width: 140px; height: auto; display: block; margin: 0 auto; }
                    .content-title { text-align: center; font-weight: bold; color: black; margin-top: 10px; font-size: 1.2em; }
                    .content { background-color: #f0f0f0; border-radius: 8px; padding: 15px; line-height: 1.5; font-size: 1em; color: black; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; font-size: 0.8em; color: #555; }
                    .bold { font-weight: bold; }
                </style>
            </head>
            <body>
                <table width="100%" bgcolor="#f3f3f3" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                        <td align="center">
                            <table class="container" cellpadding="0" cellspacing="0" border="0" align="center">
                                <tr><td class="header" align="center"><img src="https://www.getvaas.com/logos/LogoCompletoOscuro.png" alt="Logo"></td></tr>
                                <tr><td class="content-title">Buenos días.</td></tr>
                                <tr><td class="content" align="left"><p style="margin: 0; padding: 10px 0;">En nuestro rol de administradores maestros, enviamos adjunta la solicitud de distribución de fondos.</p></td></tr>
                                <tr><td style="padding: 20px 0;" align="left">
                                    <p style="margin: 0; padding: 10px 0;">Cordialmente,</p>
                                    <p class="bold" style="margin: 0; padding: 10px 0;">Equipo de Vaas</p>
                                    <p class="bold" style="margin: 0; padding: 10px 0;">Administración Maestra en Tiempo Real</p>
                                </td></tr>
                                <tr><td class="footer" align="center">
                                    <p style="margin: 0; padding: 10px 0;">© 2024 Vaas. All rights reserved.</p>
                                    <p style="margin: 0; padding: 10px 0;">If you have any questions, please contact us at support@getvaas.com</p>
                                </td></tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>""";

    private final NotificationProvider notificationProvider;

    public void execute(DistributionConfig config) {
        if (config == null || config.config() == null) {
            return;
        }

        var notifications = config.config().notifications();
        if (notifications == null || notifications.channels() == null || notifications.templates() == null) {
            return;
        }

        var enabledEvents = notifications.channels().enabledEvents();
        if (enabledEvents == null || !enabledEvents.contains(NotificationEvent.DISTRIBUTION_SUCCEEDED)) {
            return;
        }

        var recipients = notifications.templates().recipients();
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        var type = buildTemplateType(config.companyId());
        try {
            var existing = notificationProvider.getTemplates(type, NOTIFICATION_CHANNEL_ID);
            if (!existing.isEmpty()) {
                log.info("Template de notificación ya existe [type={}, id={}]", type, existing.get(0).id());
                return;
            }
            var createdId = notificationProvider.createTemplate(DEFAULT_HTML, type, NOTIFICATION_CHANNEL_ID);
            log.info("Template de notificación creado [type={}, id={}]", type, createdId);
        } catch (Exception e) {
            log.warn("No se pudo asegurar el template de notificación [type={}]: {}", type, e.getMessage(), e);
        }
    }

    private String buildTemplateType(Long companyId) {
        return companyId + "_DISTRIBUTION_SUCCEEDED";
    }
}
