**Created at**: 2026-08-27
**Status**: Approved
**Based on story**: @specs/20260827-150425_notifications-sftp-delivery-vpr-9721/story.md

# Plan: Notifications — SFTP delivery channel

### Goal
Agregar el campo `sftpDelivery` a `NotificationsConfig` (nuevo, no reemplaza ningún placeholder) y
extender `UpdateNotificationsUseCase` para persistirlo, sin duplicar el endpoint
`PUT /configs/{id}/notifications`.

### Context
- `NotificationsConfig` (VPR-9639/9640) — se agrega `sftpDelivery`, mismo nivel que `channels` y
  `templates`.
- Mismo patrón que las demás etapas: nada de credenciales/llaves en texto plano — `credentialKey`
  y `encryptionKeyRef` son referencias externas (Secrets Manager), no valores resueltos acá.
- No es un valor más de `NotificationChannel` — necesita su propio sub-modelo porque requiere
  campos de conexión, no solo un nombre de canal.

### Public Contracts
- **Domain**: `SftpDeliveryConfig(enabled, credentialKey, remotePathTemplate, fileNameTemplate,
  encryptionKeyRef)`.
- **DTO**: `UpdateSftpDeliveryRequest(enabled, credentialKey, remotePathTemplate, fileNameTemplate,
  encryptionKeyRef)`; `UpdateNotificationsRequest` extendido con `sftpDelivery`.
- **Endpoint**: sin cambios — `PUT /configs/{id}/notifications` (ya existe).
- **Tests**: extender `UpdateNotificationsUseCaseTest` con los casos de SFTP delivery.

### Phases

#### Phase 1: Modelo tipado
- [ ] `SftpDeliveryConfig` (domain)
- [ ] `NotificationsConfig.sftpDelivery` (nuevo campo)
- [ ] `UpdateSftpDeliveryRequest` (DTO) + `UpdateNotificationsRequest.sftpDelivery`

#### Phase 2: Use case + validaciones
[Extiende UpdateNotificationsUseCase — no crea un endpoint nuevo.]
- [ ] `sftpDelivery` no enviado → persiste `null` sin error
- [ ] `enabled=true` requiere `credentialKey`, `remotePathTemplate` y `fileNameTemplate`
- [ ] `enabled=false` no valida esos 3 campos, persiste lo que venga tal cual
- [ ] `encryptionKeyRef` siempre opcional, en ambos casos

#### Phase 3: Tests
- [ ] `enabled=true` con los 3 campos requeridos → persiste tal cual
- [ ] `enabled=true` con `encryptionKeyRef` → persiste tal cual
- [ ] `enabled=true` sin `credentialKey` → error
- [ ] `enabled=true` sin `remotePathTemplate` → error
- [ ] `enabled=true` sin `fileNameTemplate` → error
- [ ] `enabled=false` sin los 3 campos → no error
- [ ] `sftpDelivery` no enviado → `null`, sin error
- [ ] Preserva `channels`/`templates` sin tocar

### Next Step
Riesgos documentados, no resueltos aquí: si el wizard genera `credentialKey` nuevo por deal o
elige uno pre-provisionado; migrar el nombre del Lambda hardcodeado; la duplicación de fuente de
PGP key en el sistema real; si `protocol` (sftp/ftps) necesita exponerse.
