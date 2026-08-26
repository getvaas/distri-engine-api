**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-161306_notifications-events-channels-templates-vpr-9639-9640/story.md

# Plan: Notifications — Events, Channels y Templates

### Goal
Tipar el placeholder `notifications` de `DistributionConfigPayload` (hoy `JsonNode`) con Channels
& Events (VPR-9639) y Templates (VPR-9640), con un endpoint nuevo dedicado a esta etapa.

### Context
- `DistributionConfigPayload.notifications` pasa de `JsonNode` a `NotificationsConfig`.
- Mismo patrón que Payment Filters: un único endpoint (`PUT /configs/{id}/notifications`) para
  toda la etapa, con sub-secciones independientes (`channels`, `templates`), ambas opcionales.
- Modelo verificado contra el mockup real, no solo el texto de los tickets — ver `story.md` para
  las 2 discrepancias encontradas y corregidas.

### Public Contracts
- **Domain**: `NotificationChannel` (enum: `SLACK`, `EMAIL`, `WEBHOOK`, `ROAM`),
  `NotificationEvent` (enum: `DISTRIBUTION_SUCCEEDED`, `DISTRIBUTION_FAILED`,
  `READINESS_CHECK_FAILED`, `OWNERLESS_PAYMENT_TAPES`, `TRANSFER_INSTRUCTION_READY`),
  `NotificationChannelsConfig(channels, enabledEvents)`,
  `DocumentTemplateRef(name, fileName, description, format)`,
  `NotificationTemplatesConfig(subject, recipients, documents)`,
  `NotificationsConfig(channels, templates)`.
- **DTO**: `DocumentTemplateRefRequest`, `UpdateNotificationChannelsRequest(channels,
  enabledEvents)`, `UpdateNotificationTemplatesRequest(subject, recipients, documents)`,
  `UpdateNotificationsRequest(channels, templates)`.
- **Endpoint**: `PUT /configs/{id}/notifications` (nuevo).
- **Use case**: `UpdateNotificationsUseCase` (nuevo).
- **Tests**: `UpdateNotificationsUseCaseTest` (nuevo).

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de notifications.]
- [ ] `NotificationChannel`, `NotificationEvent`, `NotificationChannelsConfig`,
  `DocumentTemplateRef`, `NotificationTemplatesConfig`, `NotificationsConfig`
- [ ] `DistributionConfigPayload.notifications` de `JsonNode` a `NotificationsConfig`
- [ ] `DocumentTemplateRefRequest`, `UpdateNotificationChannelsRequest`,
  `UpdateNotificationTemplatesRequest`, `UpdateNotificationsRequest` (DTOs)

#### Phase 2: Use case + validaciones
- [ ] `channels`/`templates` no enviados → persisten `null` sin error (etapa opcional)
- [ ] `documents`: cada uno requiere `name` y `fileName`
- [ ] Sin validación de formato de email en `recipients` (fuera de alcance)

#### Phase 3: Endpoint
- [ ] `PUT /configs/{id}/notifications` en `DistributionConfigRouter`

#### Phase 4: Tests
- [ ] `channels`/`enabledEvents` enviados → persisten tal cual
- [ ] `subject`/`recipients`/`documents` válidos → persisten tal cual
- [ ] Documento sin `name` o sin `fileName` → error
- [ ] `channels`/`templates` no enviados → `null` sin error
- [ ] Preserva el resto del payload sin tocar

### Next Step
Gaps documentados, no resueltos aquí: `body` bloqueado por notifications-api; SFTP (E7b) sin
lugar en la config; recipients de 2 audiencias (caso Inklusiva) si aparece una necesidad real.
