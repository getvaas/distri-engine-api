**Created at**: 2026-09-21
**Status**: In Progress
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

### Next Step
Ambas fases implementadas y compilando limpio. Falta correr `./scripts/run-tests.sh` y confirmar que pasan antes de marcar Status Done.
