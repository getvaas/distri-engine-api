# Original Request

Ticket: VPR-9671 (https://pmvaas1.atlassian.net/browse/VPR-9671) — "envío de instrucción de transferencia y notificación", la tarea que faltaba para cerrar el bloque de reportes (VPR-9668/9669/9670).

## Contexto del pedido del usuario

- "ok, ya lo envie y probe.. asi que si no hay nada mas que completar en este ticket [VPR-9670], podemos avanzar a este otro: VPR-9671 que tiene el detalle para envio de instruccion de transferencia y notificacion... era la tarea que nos faltaba para esto de los reportes..."
- Elegido explícitamente: "Construir el cliente HTTP real a notifications-api" (no mockear, no diferir).
- Confirmado que el riesgo de registro/pre-registro de templates de notificación no es un bloqueante de este lado: "Sí, ya sé cómo resolverlo / no es un problema".
- Aclaración de scope: "el ticket 71, es otro issue, aca se sube una iteracion funcional.. y luego se avanza con la mejora." — se construye una primera iteración funcional (cliente real + envío básico), dejando mejoras (adjuntar reporte, multi-canal SFTP+PGP) para después.

## Investigación real ya hecha (no repetir)

- `master-trust-servicer-api` (Kotlin) llama a un microservicio externo `notifications-api`: `POST /notifications` con `{type, recipients, context, attachmentsUrls}`, autenticado con Bearer token Auth0 client-credentials (M2M).
- El repo hermano `conciliation-engine-api` (Java) ya implementa este mismo patrón contra otro microservicio VAAS: `Auth0MachineToMachineClient` (Spring `RestClient`, token cacheado con eviction programada `@Scheduled` cada 20hs) + un cliente HTTP dedicado al recurso. Es la referencia directa a reusar/adaptar.
- `distri-engine-api` ya tiene el esqueleto de configuración Auth0 (`infra-config.yml`) pre-cargado pero sin usar por ningún código todavía.
- No existe en `distri-engine-api`, antes de esta historia, ningún cliente HTTP hacia servicios externos ni lógica de notificación real — solo clases de configuración tipadas (`NotificationsConfig`, `NotificationChannelsConfig`, `NotificationTemplatesConfig`, enums).
- El SFTP+PGP (`SFTPSender` vía AWS Lambda) existe en el sistema real pero como un flujo completamente separado y desconectado del `DistributionNotificationManager` estándar — confirmado que es un gap real incluso en producción, no algo que le falte solo a distri-engine-api.
