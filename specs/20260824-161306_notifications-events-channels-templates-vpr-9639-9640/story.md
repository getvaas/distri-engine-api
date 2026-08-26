**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9639 @https://pmvaas1.atlassian.net/browse/VPR-9640
**Plan implemented**: —

# Story: Notifications — Events, Channels y Templates

### Description
Cada deal necesita elegir por dónde se avisa (Slack, Email, Webhook, ROAM) y qué eventos disparan
esos avisos, además del contenido del mensaje de instrucción: asunto, destinatarios y los
documentos adjuntos que se generan y se envían junto al aviso.

### Acceptance Criteria
- [ ] **Given** una lista de `channels` seleccionados, **When** se guarda, **Then** persiste tal
  cual (lista plana, sin restricción de combinación).
- [ ] **Given** una lista de `enabledEvents`, **When** se guarda, **Then** persiste tal cual —
  no hay mapeo evento→canal individual, los eventos habilitados notifican por todos los canales
  seleccionados.
- [ ] **Given** `subject` y `recipients` (lista de emails), **When** se guarda, **Then** persisten
  tal cual.
- [ ] **Given** una lista de `documents` (adjuntos) con `name`/`fileName`, **When** se guarda,
  **Then** persiste tal cual.
- [ ] **Given** un documento sin `name` o sin `fileName`, **When** se intenta guardar, **Then**
  se rechaza.
- [ ] **Given** ninguna de las 2 sub-secciones (`channels`/`templates`) enviada, **When** se
  guarda, **Then** persisten `null` sin error — la etapa completa es opcional.

### Additional Context
Verificado contra el mockup real (`docs/distribution-engine-onboarding.html` STEP 7), no solo el
texto de los tickets — encontró 2 discrepancias reales que cambiaron el modelo planeado:

1. El mockup mezcla canales internos (Slack, ROAM — "Internal notifications" en la propia UI) con
   el canal cliente (Email) en un solo selector plano, sin SFTP. Se sigue el mockup literal, no la
   interpretación "solo instrucción al cliente" que se había considerado antes de revisarlo.
2. El mockup muestra un solo campo `Recipients` (lista plana), no dos grupos de audiencia
   (cliente/lender) como pedía el texto de VPR-9640 — el caso real de Inklusiva (dos audiencias
   por corrida) queda como gap conocido, no resuelto, a extender si aparece una necesidad real.

`body` (cuerpo del mensaje) queda explícitamente fuera de esta historia — está bloqueado por una
pregunta sin responder sobre notifications-api (¿editable sin deploy, y por quién?), no es una
decisión de negocio que podamos tomar acá. `format` de cada documento adjunto es `String` libre
(PDF/XLSX vistos en el mockup, pueden aparecer más). SFTP (E7b) sigue sin lugar en la config —
pendiente para cuando un deal real lo necesite.
