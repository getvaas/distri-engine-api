**Created at**: 2026-08-31
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9641
**Plan implemented**: @specs/20260831-151429_review-activate-deactivate-config-vpr-9641/plan.md

# Story: Activar o desactivar una config mediante un único endpoint

### Description
El wizard ya podía activar una config (`ActivateDistributionConfigUseCase`, VPR-9644), pero no
existía una acción para desactivarla. En vez de agregar un segundo endpoint separado
(`/deactivate`), se unificó activar y desactivar en un único endpoint que recibe el status
deseado — evita duplicar la forma "acción con efecto sobre el estado" en dos rutas distintas.

### Acceptance Criteria
- [x] **Given** `status=ACTIVE` sin otra config `ACTIVE` para el mismo `companyId`, **When** se
  actualiza, **Then** la config pasa a `ACTIVE`.
- [x] **Given** `status=ACTIVE` con otra config ya `ACTIVE` para el mismo `companyId`, **When** se
  actualiza, **Then** la otra config pasa a `INACTIVE` — nunca hay dos `ACTIVE` a la vez
  (VPR-9644).
- [x] **Given** `status=INACTIVE` sobre una config `ACTIVE`, **When** se actualiza, **Then** pasa
  a `INACTIVE` sin afectar ninguna otra config.
- [x] **Given** `status=INACTIVE` sobre una config ya `INACTIVE`, **When** se actualiza, **Then**
  no hay error — la operación es idempotente.
- [x] **Given** `status=DRAFT` o `status` no enviado, **When** se intenta actualizar, **Then** se
  rechaza — `DRAFT` no es un target válido para este endpoint.

### Additional Context
Reemplaza los endpoints separados `POST /configs/{id}/activate` y `POST /configs/{id}/deactivate`
por uno solo: `PUT /configs/{id}/status`, recibiendo `{status: ACTIVE | INACTIVE}`. Se decidió así
en vez de mantener verbos separados porque, aunque `ACTIVE` tiene un efecto secundario real
(desactivar hermanos) que un setter genérico podría esconder, se priorizó tener un único punto de
entrada — el efecto secundario queda documentado en el código y en este doc en vez de en el nombre
del endpoint.

`ActivateDistributionConfigUseCase` y `DeactivateDistributionConfigUseCase` (esta última nunca
llegó a mergearse) se reemplazan por `UpdateDistributionConfigStatusUseCase`, que concentra ambos
comportamientos.

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
