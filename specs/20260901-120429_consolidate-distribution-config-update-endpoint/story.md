**Created at**: 2026-09-01
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: —

# Story: Consolidar la creación y actualización de DistributionConfig en endpoints full-payload

### Description
Hoy `DistributionConfigRouter` expone un `PUT` separado por cada nodo del wizard de configuración
(pool, payment filters, distribution rules, ownership, readiness checks, notifications, transfer
instructions, virtual columns), cada uno con su propio use case que hace merge parcial contra el
`config_json` existente. Este patrón de "actualización por bloque" no aporta valor real: el
front-end ya mantiene un único estado de formulario para todo el wizard, y si el producto deja de
ser wizard-first el día de mañana, la separación por bloques obliga a rehacer este trabajo de
unificación. Se busca consolidar en un único punto de entrada por operación (crear / actualizar)
que reciba la estructura completa del config y la trate como fuente de verdad, sin merge implícito
con el valor anterior.

### Acceptance Criteria
- [ ] **Given** un `DistributionConfig` existente en cualquier estado, **When** se llama a `PUT /configs/{id}` con los 8 nodos (pool, paymentFilters, distributionRules, ownership, readinessChecks, notifications, transferInstructions, virtualColumns) en el body, **Then** el `config_json` guardado refleja exactamente esos 8 nodos construidos con las mismas reglas de validación/defaults que tenían los endpoints por bloque hoy.
- [ ] **Given** un `DistributionConfig` existente con un nodo previamente configurado (p.ej. `pool` no nulo), **When** se llama a `PUT /configs/{id}` sin ese nodo en el body (`pool` ausente/null), **Then** el nodo queda `null` en el config guardado — no se preserva el valor anterior.
- [ ] **Given** un `PUT /configs/{id}` con `name`/`masterTrustId`/`country`/`currency` parcialmente ausentes, **When** se procesa el request, **Then** esos campos de Deal Info conservan su valor existente (comportamiento de fallback sin cambios).
- [ ] **Given** un `POST /configs`, **When** el body incluye opcionalmente los 8 nodos del wizard además de los datos mínimos de deal info, **Then** el `DistributionConfig` creado persiste esos nodos ya construidos y validados, sin requerir llamadas adicionales de actualización.
- [ ] **Given** un request a cualquiera de los 7 endpoints `PUT /configs/{id}/{pool|payment-filters|distribution-rules|ownership|readiness-checks|notifications|transfer-instructions|virtual-columns}`, **When** se invoca, **Then** el endpoint ya no existe (404 / sin mapping) — quedan eliminados del router.
- [ ] **Given** un request inválido para cualquier nodo (mismas condiciones de validación que hoy: p.ej. `ACCOUNT_BALANCE` sin `accounts`, `hasAccountingPayments=true` sin `conditionGroups`, `sftpDelivery.enabled=true` sin `credentialKey`, etc.), **When** se procesa el `PUT`/`POST` consolidado, **Then** se rechaza con el mismo tipo de excepción (`InvalidDistributionConfigException`) y mensaje que el endpoint por bloque original.
- [ ] **Given** `PUT /configs/{id}/status`, **When** se invoca, **Then** su comportamiento no cambia (queda fuera de este refactor).

### Additional Context
- No hay ticket VPR asignado — es un refactor de arquitectura pedido directamente por el usuario (dueño del negocio), no una historia de Jira preexistente.
- No rompe consumidores reales: el único cliente en desarrollo (wizard v2 de `vaas-backoffice`) todavía está mockeado, sin conectarse a la API real.
- El formato de `config_json` persistido en base de datos no cambia — este refactor es puramente de superficie de API (DTOs de request + orquestación), no de esquema de datos.
- La lógica de validación/construcción por nodo (documentada en `docs/architecture/distribution-config-schema.md`) se preserva exactamente igual, solo cambia dónde vive (se extrae de cada `Update*UseCase` a un componente reusable por nodo) y quién decide entre "construir" vs. "dejar null" (ya no hay merge con `existing`).
