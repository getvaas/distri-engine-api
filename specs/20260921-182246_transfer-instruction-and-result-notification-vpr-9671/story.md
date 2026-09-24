**Created at**: 2026-09-21
**Status**: Done
**Original input**: https://pmvaas1.atlassian.net/browse/VPR-9671
**Plan implemented**: @plan.md

# Story: Notificar el resultado de una distribución vía notifications-api

### Description
Hoy, cuando el motor termina de correr una distribución (persistencia + reporte), nadie se entera: no existe ningún cliente HTTP ni de mensajería que avise el resultado hacia afuera. En el sistema real, ese aviso se hace llamando a `notifications-api` (un microservicio externo autenticado con Auth0 M2M) con la instrucción de transferencia y el resultado de la distribución. Esta historia agrega esa integración como una primera iteración funcional: el motor envía una notificación real después de correr una distribución, dejando la mejora de canales adicionales (por ejemplo SFTP+PGP) fuera de este alcance para una iteración posterior.

### Acceptance Criteria
- [ ] **Given** una distribución que terminó de persistirse exitosamente, **When** el motor la corre vía `/distributions/run`, **Then** se envía una notificación a `notifications-api` con el resultado de la distribución (autenticada con un token Auth0 M2M válido).
- [ ] **Given** que el token Auth0 cacheado expiró o no existe, **When** se necesita notificar, **Then** el cliente obtiene un token nuevo antes de llamar a `notifications-api`.
- [ ] **Given** que `notifications-api` responde con error o no responde, **When** ocurre esa falla, **Then** la distribución ya persistida no se revierte ni falla — el error de notificación se registra (log) sin interrumpir el flujo de `/distributions/run`.
- [ ] **Given** una distribución corrida con éxito, **When** se arma el payload de notificación, **Then** incluye el tipo de notificación, destinatarios resueltos de la config del deal, y el contexto del resultado (sin adjuntar todavía el reporte Excel de VPR-9670 — eso es la mejora de una iteración posterior).

### Additional Context
- Mecanismo real verificado contra `master-trust-servicer-api`: `notifications-api` expone `POST /notifications` (`{type, recipients, context, attachmentsUrls}`), autenticado con Bearer token obtenido vía Auth0 client-credentials (M2M).
- Ya existe una implementación Java de referencia directamente reusable del mismo patrón en el repo hermano `conciliation-engine-api` (`Auth0MachineToMachineClient` con `RestClient`, token cacheado con eviction programada).
- `distri-engine-api` ya tiene el esqueleto de configuración Auth0 en `infra-config.yml`, hoy sin usar.
- Fuera de alcance de esta iteración (queda para la "mejora" posterior): adjuntar el reporte Excel de VPR-9670 a la notificación, canal SFTP+PGP multi-canal (confirmado que ni siquiera el sistema real lo integra con el flujo estándar de notificación), reintentos/colas de notificación.
