**Created at**: 2026-09-24
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Notificar el resultado de una distribución vía notifications-api

### Executive Summary
El motor ahora avisa de verdad, por email, cuando una distribución se corre con éxito — llamando al mismo microservicio (`notifications-api`) y con el mismo mecanismo de autenticación (Auth0 M2M) que usa el sistema real, incluyendo el paso que el sistema real hace pero que fácilmente se pasa por alto: asegurar que exista un template de notificación antes de intentar mandarla, para que el aviso no falle silenciosamente por falta de configuración del lado del proveedor externo.

### Technical Summary
- Nuevo puerto `NotificationProvider` (`domain/port/`) con 3 operaciones: `notify`, `getTemplates`, `createTemplate` — implementado por `NotificationApiClient` (`infrastructure/client/`), autenticado vía `Auth0MachineToMachineClient` (token M2M cacheado, mismo patrón que `conciliation-engine-api`).
- `NotifyDistributionResultUseCase`: al final de una corrida exitosa (`RunDistributionUseCase`), si el deal tiene `notifications.channels`/`templates` configurados y `DISTRIBUTION_SUCCEEDED` habilitado, notifica con los recipients del deal — sin adjuntar todavía el reporte de VPR-9670 (mejora futura), y sin propagar nunca un fallo de notificación hacia una distribución ya persistida.
- `EnsureNotificationTemplateUseCase` (agregado durante la implementación, no en el plan original): réplica de `createDefaultNotificationTemplate` del sistema real (`CreateDistributionConfig.kt`) — al crear una `DistributionConfig` con notificaciones configuradas, verifica si existe un template en `notifications-api` para `"<companyId>_DISTRIBUTION_SUCCEEDED"`+`channelId=3` y lo crea (HTML default copiado literal del sistema real) si no existe. Corre una sola vez, en la creación de la config — mismo momento que el sistema real, no en cada corrida ni en un update posterior.
- `getTemplates`/`createTemplate` verificados contra el código Kotlin real (`NotificationHttpProvider.kt`): la respuesta de `GET /templates` viene envuelta en `{"data": [...]}`, la query real solo manda `type`+`page`+`limit` (el filtro por `channelId` es del lado del cliente) — no es el shape que se había asumido inicialmente por analogía con `conciliation-engine-api`.
- **Límite conocido, importante para pruebas manuales**: `EnsureNotificationTemplateUseCase` solo corre vía `POST /configs` (`CreateDistributionConfigUseCase`) — los `examples/scenario-0N-*.sql` insertan `distribution_engine_config` directo por SQL, así que no les crea el template retroactivamente.
- Nuevo bloque `notifications-api.base-url` en `infra-config.yml`.
- Tests: `Auth0MachineToMachineClientTest`, `NotificationApiClientTest` (6 casos, incluye `getTemplates`/`createTemplate`), `NotifyDistributionResultUseCaseTest` (5 casos), `EnsureNotificationTemplateUseCaseTest` (7 casos), casos nuevos en `RunDistributionUseCaseTest` y `CreateDistributionConfigUseCaseTest`. Corridos vía `./scripts/run-tests.sh`, `BUILD SUCCESSFUL` confirmado por el usuario.

### Phases Completed
- [x] **Phase 1: Cliente Auth0 M2M + cliente notifications-api** — `Auth0MachineToMachineClient`, `NotificationApiClient` (`notify`), config nueva.
- [x] **Phase 2: Use case de notificación + wiring en RunDistributionUseCase** — `NotifyDistributionResultUseCase`, gating por config/evento, sin propagar fallos.
- [x] **Phase 3: Auto-creación del template en notifications-api** — `EnsureNotificationTemplateUseCase`, `getTemplates`/`createTemplate`, wireado en `CreateDistributionConfigUseCase` (gap encontrado durante pruebas manuales, no en el plan original).
