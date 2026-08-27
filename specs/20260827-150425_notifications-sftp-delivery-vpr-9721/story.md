**Created at**: 2026-08-27
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9721
**Plan implemented**: —

# Story: Notifications — SFTP delivery channel

### Description
Algunos deals (Liquitech hoy, Skandia como caso de validación) entregan la instrucción de
transferencia por SFTP en vez de (o además de) email. El deal necesita poder configurar esa
entrega — a qué credencial referenciar, a qué ruta remota, con qué nombre de archivo, y con qué
llave de cifrado — sin que la config guarde credenciales ni llaves en texto plano.

### Acceptance Criteria
- [ ] **Given** `enabled=true` con `credentialKey`, `remotePathTemplate` y `fileNameTemplate`,
  **When** se guarda, **Then** persiste tal cual.
- [ ] **Given** `encryptionKeyRef` sin especificar, **When** se guarda, **Then** queda `null` — es
  opcional (no todos los canales SFTP cifran hoy).
- [ ] **Given** `enabled=true` sin `credentialKey`, `remotePathTemplate` o `fileNameTemplate`,
  **When** se intenta guardar, **Then** se rechaza — sin esos 3 campos no hay forma de resolver la
  entrega.
- [ ] **Given** `enabled=false`, **When** se guarda, **Then** el resto de los campos se persisten
  tal cual si vienen, sin forzar ninguno — no tiene sentido validar de más algo que está apagado.
- [ ] **Given** `sftpDelivery` no enviado, **When** se guarda el resto de `notifications`, **Then**
  persiste `null` sin error — es una sub-sección opcional más.

### Additional Context
Verificado contra el código real: el motor de producción no abre la conexión SFTP directamente —
invoca un Lambda compartido (`prod-sftp-orchestrator-lambda`, hoy hardcodeado) pasándole
`facility_id`/`s3_path`/`remote_path`; el Lambda resuelve host/user/pass/protocol/public_key desde
AWS Secrets Manager por ese `facility_id`. Por eso `credentialKey` es una referencia (el
`facility_id`/`sftpCredentialKey` que ya existe hardcodeado por borrower hoy), nunca credenciales
inline — mismo criterio para `encryptionKeyRef` con la llave PGP.

`remotePathTemplate`/`fileNameTemplate` son `String` con placeholders (ej.
`/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/`) — esta historia solo tipa el string de la
plantilla, el parseo/interpolación de los placeholders es responsabilidad de la etapa de
ejecución, no de esta historia.

Vive como campo nuevo `sftpDelivery` dentro de `NotificationsConfig`, junto a `channels`
(VPR-9639) y `templates` (VPR-9640) — no es un valor más del enum `NotificationChannel`, porque
necesita una referencia de conexión estructurada, no solo un nombre de canal.

Explícitamente fuera de esta historia, documentado como riesgo abierto: si el wizard genera un
`credentialKey` nuevo por deal o elige uno pre-provisionado; migrar el nombre del Lambda
hardcodeado a configuración; la inconsistencia real detectada de que la PGP key tiene hoy 2
fuentes independientes (config Kotlin vs. Secrets Manager); si `protocol` (sftp vs. ftps) necesita
exponerse en el wizard.
