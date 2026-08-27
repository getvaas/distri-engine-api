# Original Request

**Source**: Jira ticket [VPR-9721](https://pmvaas1.atlassian.net/browse/VPR-9721) — "Notifications: SFTP delivery channel"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-27, post-refinamiento)

Canal de entrega por SFTP, gap ya anotado en VPR-9639. Verificado contra el código real: el motor
no abre la conexión SFTP directamente — invoca un Lambda compartido
(`prod-sftp-orchestrator-lambda`, hardcodeado) con `{facility_id, s3_path, remote_path}`; el
Lambda (Python + `paramiko`, fallback FTPS) resuelve credenciales desde AWS Secrets Manager por
`facility_id`. `facility_id`/`sftpCredentialKey` hoy hardcodeado por borrower en Kotlin
(Liquitech, Finamco). No es infraestructura nueva por corrida — es compartida, seleccionada por
`facility_id`. Inconsistencia real detectada: la PGP key tiene 2 fuentes independientes hoy (env
var en config Kotlin vs. secret en Secrets Manager).

Modelo:
```
SftpDeliveryConfig
├── enabled: boolean
├── credentialKey: String        — facility_id, referencia a Secrets Manager
├── remotePathTemplate: String   — ej. "/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/"
├── fileNameTemplate: String     — ej. "{prefix}{ddMMyyyy}{HHmmss}{accountLast4}"
└── encryptionKeyRef: String?    — referencia a la key PGP, nunca en texto plano
```

## User additions (this session)

- El usuario compartió una comparación detallada de 2 patrones legacy (business.yml estático, y
  `NotificationConfigV2`/`NotificationChannelConfig` en `distribution_config` Kotlin v2 — este
  último con `channel: String` ya libre, no enum cerrado) y una primera propuesta de
  `SftpChannelConfig` (con `remotePathTemplate`, `fileNameTemplate`, `encryptionKeyRef`) que
  mejoró el modelo original (que solo tenía `remotePath` plano, sin templates ni referencia PGP).
- Confirmado: esto se modela en `distri-engine-api` (Java), no se extiende el `distribution_config`
  legacy (Kotlin) — este proyecto lo reemplaza. Se sigue el patrón de records tipados ya
  establecido (`OwnershipConfig`, `NotificationsConfig`), no un mapa genérico
  `Map<String,String>` como en el Kotlin legacy.
- Confirmado: la PGP key se externaliza como referencia (`encryptionKeyRef`), nunca en texto
  plano — mismo criterio que `credentialKey`.
- VPR-9713 (owner_dictionary.json) se confirmó 100% externo — el path S3 es una env var global de
  infraestructura, no algo que el wizard configure por deal. No requiere ningún campo nuevo en
  `distri-engine-api`.
