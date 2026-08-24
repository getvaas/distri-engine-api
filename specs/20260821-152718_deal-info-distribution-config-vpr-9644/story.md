**Created at**: 2026-08-21
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9644
**Plan implemented**: @specs/20260821-152718_deal-info-distribution-config-vpr-9644/plan.md

# Story: Deal Info — persistir la config base de una distribución

### Description
El Distribution Engine necesita un punto de entrada para crear y editar la configuración de un deal:
borrower, master trust, país, moneda y nombre. Es el primer paso del wizard de 9 etapas y la base de la
que dependen todas las demás (Pool Strategy, Payment Filters, Ownership, etc.) — sin una config
persistida no hay nada sobre lo que construir el resto.

### Acceptance Criteria
- [x] **Given** un borrower con companyId y master trust conocidos, **When** se crea una config con
  nombre/país/moneda, **Then** queda persistida en estado DRAFT.
- [x] **Given** una config existente, **When** se pide por id, **Then** se devuelven sus campos de Deal
  Info deserializados correctamente desde `config_json`.
- [x] **Given** una config existente, **When** se actualiza solo el nombre o el master trust, **Then**
  el resto del `config_json` (secciones todavía no definidas) queda intacto.
- [x] **Given** un id inexistente, **When** se pide o actualiza, **Then** responde 404 con el mensaje
  del error.

### Additional Context
Mismo patrón que `conciliation_engine_config` en `conciliation-engine-api`: entidad delgada (id, name,
companyId, masterTrustId, status, config_json) + un record de dominio rico deserializado del JSON. Las
secciones que el wizard todavía no define (pool, distributablePayments, virtualColumns, rules, ownership,
readinessChecks, notifications) se guardan como `JsonNode` crudo para no perder datos ni bloquear este
ticket — se van tipando ticket por ticket a medida que se definen (ver VPR-9628 como el primer caso).
