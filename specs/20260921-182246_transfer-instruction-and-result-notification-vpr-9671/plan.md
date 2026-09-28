**Created at**: 2026-09-21
**Status**: Done
**Based on story**: @story.md

# Plan: Notificar el resultado de una distribución vía notifications-api

### Goal
Primera iteración funcional: el motor llama de verdad a `notifications-api` (autenticado con Auth0 M2M) para avisar el resultado de una distribución exitosa, después de persistirla. Sin adjuntar el reporte Excel de VPR-9670 ni resolver canales adicionales (SFTP+PGP) — eso queda para una mejora posterior.

### Context
- `domain/port/` existe vacío (solo `.gitkeep`) — es el lugar ya reservado para el puerto `NotificationProvider`.
- `NotificationsConfig(channels, templates, sftpDelivery)` y sus sub-records (`NotificationChannelsConfig{channels, enabledEvents}`, `NotificationTemplatesConfig{subject, recipients, documents}`) ya están tipados (VPR-9639/9640) y son todos opcionales — un deal puede no tener nada configurado.
- `NotificationEvent` ya tiene `DISTRIBUTION_SUCCEEDED`/`DISTRIBUTION_FAILED`/`TRANSFER_INSTRUCTION_READY`/etc. — se reusa como el `type` del payload de notificación.
- `infra-config.yml` ya tiene el bloque `auth0` (url/client-id/client-secret) sin usar. Falta agregar `notifications-api.base-url`.
- Referencia directa a adaptar (repo hermano `conciliation-engine-api`): `infrastructure/client/Auth0MachineToMachineClient.java` (`RestClient`, token cacheado, eviction `@Scheduled` cada 20hs) y `infrastructure/client/NotificationApiClient.java` (`POST /notifications`, header `Authorization: Bearer <token>`).
- `RunDistributionUseCase.execute()` hoy termina en `markPaymentTapesAsDistributedUseCase.execute(...)` — ahí se agrega la llamada a notificar, después de persistir.
- `DistributionExecutionResult` no se modifica — la notificación es un efecto secundario, no cambia el contrato de retorno del endpoint.

### Public Contracts
- **Domain port**: `domain/port/NotificationProvider.java` — `void notify(String type, List<String> recipients, Map<String, String> context, List<String> attachmentsUrls)`.
- **Infrastructure**:
  - `infrastructure/client/Auth0MachineToMachineClient.java` — token M2M cacheado (mismo patrón que `conciliation-engine-api`, adaptado a este paquete), usa `auth0.url`/`auth0.client-id`/`auth0.client-secret` ya existentes.
  - `infrastructure/client/NotificationApiClient.java implements NotificationProvider` — `POST {notifications-api.base-url}/notifications`, body `{type, recipients, context, attachmentsUrls}`, header `Authorization: Bearer <token>` vía `Auth0MachineToMachineClient`.
  - `infra-config.yml` — nuevo bloque `notifications-api.base-url: ${NOTIFICATIONS_API_URL:${BASE_URL:http://localhost:8080/api}/notifications-bot}` (mismo formato que `conciliation-engine-api`).
- **Application**:
  - `application/usecase/NotifyDistributionResultUseCase.execute(NotificationsConfig config, Long companyId, DistributionExecutionResult result): void` — si `config`, `config.channels()` o `config.templates()` es `null`, o `DISTRIBUTION_SUCCEEDED` no está en `enabledEvents()`, no hace nada (deal sin notificaciones configuradas). Si aplica: arma `context` básico (`companyId`, `distributionId`, `assignmentsCount`) y `recipients = templates.recipients()`, llama a `notificationProvider.notify("DISTRIBUTION_SUCCEEDED", recipients, context, List.of())` (sin adjuntos todavía — `attachmentsUrls` vacío, es la mejora futura). Atrapa cualquier excepción del `NotificationProvider` y solo la loguea (WARN) — nunca la propaga, para no romper una distribución ya persistida (AC #3).
  - `RunDistributionUseCase` — agrega la llamada a `notifyDistributionResultUseCase.execute(config, companyId, result)` justo antes del `return`, solo en el camino donde `readiness.readyToDistribute()` es `true`.
- **Tests**:
  - `Auth0MachineToMachineClientTest` — token cacheado, refresh solo si no hay token, falla explícita si Auth0 no devuelve `access_token`.
  - `NotificationApiClientTest` — arma el request correcto, header Authorization correcto.
  - `NotifyDistributionResultUseCaseTest` — no notifica si config/channels/templates es null; no notifica si el evento no está en `enabledEvents`; notifica con los recipients/context correctos cuando corresponde; una excepción del provider no se propaga (se loguea y sigue).
  - `RunDistributionUseCaseTest` (existente) — agregar caso: se llama a `notifyDistributionResultUseCase` solo cuando la distribución se persiste.

### Phases

#### Phase 1: Cliente Auth0 M2M + cliente notifications-api
- [x] Agregar bloque `notifications-api.base-url` a `infra-config.yml`.
- [x] Crear `domain/port/NotificationProvider.java`.
- [x] Crear `infrastructure/client/Auth0MachineToMachineClient.java` (adaptado de `conciliation-engine-api`).
- [x] Crear `infrastructure/client/NotificationApiClient.java implements NotificationProvider`.
- [x] Tests: `Auth0MachineToMachineClientTest`, `NotificationApiClientTest`.

#### Phase 2: Use case de notificación + wiring en RunDistributionUseCase
- [x] Crear `application/usecase/NotifyDistributionResultUseCase.java` (gating por config/evento, try/catch interno, sin adjuntos).
- [x] Wirear la llamada al final de `RunDistributionUseCase.execute()` (solo camino exitoso).
- [x] Tests: `NotifyDistributionResultUseCaseTest`, caso nuevo en `RunDistributionUseCaseTest`.

#### Phase 3: Auto-creación del template en notifications-api (2026-09-24)
Gap encontrado testeando manualmente: en el sistema real, `CreateDistributionConfig.execute()`
(`CreateDistributionConfig.kt:34,52-76`) asegura que exista un template en `notifications-api`
para el `type`+`channelId` de la notificación ANTES de que cualquier corrida intente notificar —
si no existe, lo crea con un HTML default. Sin este paso, `notify()` de VPR-9671 le pega a
`notifications-api` esperando un template que puede no existir nunca. Verificado contra el código
real (`NotificationHttpProvider.kt`, `NotificationProvider.kt`): la respuesta de `GET /templates`
viene envuelta en `{"data": [...]}` (no una lista plana), el filtro por `channelId` es del lado
del cliente (la query real solo manda `type`+`page`+`limit`), y `channelId=3` + el HTML default
fueron confirmados por el usuario copiando el texto real de `CreateDistributionConfig.kt:80-119`.
- [x] Ampliar `domain/port/NotificationProvider.java`: `getTemplates(type, channelId)`,
      `createTemplate(text, type, channelId)`.
- [x] `domain/model/NotificationTemplate.java` (record: `id`, `type`, `channelId`).
- [x] Implementar ambos métodos en `NotificationApiClient` (`GET /templates?type=&page=0&limit=1`
      + envoltorio `data`, `POST /templates` con `channel_id` snake_case).
- [x] `application/usecase/EnsureNotificationTemplateUseCase.java` — mismo gating que
      `NotifyDistributionResultUseCase` (notifications/channels/templates presentes, evento
      `DISTRIBUTION_SUCCEEDED` habilitado, recipients no vacío); `type = "<companyId>_DISTRIBUTION_SUCCEEDED"`,
      `channelId=3`; `getTemplates` primero, `createTemplate` con el HTML real solo si no existe;
      excepción del provider se loguea (WARN) y no se propaga.
- [x] Wirear en `CreateDistributionConfigUseCase.execute()`, al final, sobre la config ya creada
      — mismo momento que el sistema real (no en update, no en cada corrida).
- [x] Tests: 2 nuevos en `NotificationApiClientTest` (`getTemplates` con envoltorio+filtro,
      `createTemplate` éxito/sin id), `EnsureNotificationTemplateUseCaseTest` (7 casos), caso
      nuevo en `CreateDistributionConfigUseCaseTest`.

**Límite conocido, importante para probar**: este fix solo corre dentro de
`CreateDistributionConfigUseCase` (vía `POST /configs`) — los escenarios de prueba
(`examples/scenario-0N-*.sql`) insertan `distribution_engine_config` directo por SQL, sin pasar
por ese use case, así que **no** les crea el template retroactivamente. Para que scenario-06
efectivamente encuentre/cree el template hace falta crear esa config vía `POST /configs` en vez
de (o además de) el INSERT directo.

### Next Step
All phases completed. See resume.md.
