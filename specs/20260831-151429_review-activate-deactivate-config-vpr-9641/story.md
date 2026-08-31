**Created at**: 2026-08-31
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9641
**Plan implemented**: @specs/20260831-151429_review-activate-deactivate-config-vpr-9641/plan.md

# Story: Agregar capacidad de desactivar una config

### Description
El wizard ya puede activar una config (`ActivateDistributionConfigUseCase`, VPR-9644), pero no
existe una acción para desactivarla. El operador necesita poder desactivar una config directamente
— por ejemplo, para dejar sin config activa a un borrower sin tener que activar una nueva.

### Acceptance Criteria
- [x] **Given** una config con status `ACTIVE`, **When** se desactiva, **Then** su status pasa a
  `INACTIVE`.
- [x] **Given** una config con status `INACTIVE`, **When** se desactiva de nuevo, **Then** no hay
  error — la operación es idempotente.
- [x] **Given** una config con status `DRAFT`, **When** se desactiva, **Then** su status pasa a
  `INACTIVE` sin error — mismo criterio permisivo que activate, que tampoco valida el status
  actual antes de activar.

### Additional Context
Es la contraparte directa de `ActivateDistributionConfigUseCase`, ya implementado. A diferencia de
activate, desactivar NO tiene lógica de "hermanos" — activate garantiza que nunca haya dos
`ACTIVE` para el mismo `companyId` a la vez (decisión resuelta en VPR-9644), pero desactivar una
config no afecta a ninguna otra.

Endpoint: `POST /configs/{id}/deactivate`, mismo estilo que `POST /configs/{id}/activate` — es una
acción sobre el estado de la entidad, no una actualización de `config_json` vía `PUT`.

**Explícitamente fuera de esta historia** (parte del ticket original VPR-9641, "Review & Activate",
sin resolver todavía):
- Si la pantalla de confirmación final corre una simulación/dry-run contra datos históricos antes
  de activar.
- Si reemplazar una config existente para el mismo borrower versiona o sobreescribe, y si el
  wizard debería soportar dos configs `ACTIVE` simultáneas para un mismo borrower (relevante para
  Finkargo Colombia, cuenta/tránsito) — hoy el código fuerza una sola `ACTIVE` por company
  (VPR-9644), esta historia no cambia esa regla.

Esta historia es puramente sobre el estado de la entidad `DistributionConfig` (`status`), no sobre
el payload `config_json` — no toca ningún nodo del wizard.
