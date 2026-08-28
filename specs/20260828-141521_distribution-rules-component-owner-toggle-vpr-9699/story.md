**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9699
**Plan implemented**: @specs/20260828-141521_distribution-rules-component-owner-toggle-vpr-9699/plan.md

# Story: Habilitar o deshabilitar la asignación de owner por componente de cuota

### Description
Un deal puede o no necesitar dividir su cuota en componentes (principal, interés, etc.) con un
owner distinto para cada uno (VPR-9643). Hoy la config no tiene forma explícita de decir "este
deal no usa esta funcionalidad" — solo se infiere de que la lista esté vacía. El deal necesita
poder declarar explícitamente si activa o no la asignación de owner por componente.

### Acceptance Criteria
- [x] **Given** `hasComponentOwners=true` con una o más reglas en `componentOwners`, **When** se
  guarda la config, **Then** persiste tal cual.
- [x] **Given** `hasComponentOwners=false`, **When** se guarda la config (sin importar si
  `componentOwners` viene vacía o con datos), **Then** persiste sin error — se permiten cargas
  parciales/inconsistentes mientras la config esté en `DRAFT`, mismo criterio que el resto del
  proyecto.
- [x] **Given** una regla sin `component` o sin `owner`, **When** se intenta guardar, **Then** se
  rechaza — validación ya existente de VPR-9643, sin cambios.
- [x] **Given** dos reglas con el mismo `component`, **When** se intenta guardar, **Then** se
  rechaza — validación ya existente de VPR-9643, sin cambios.

### Additional Context
No hay validación cruzada entre `hasComponentOwners` y el contenido de `componentOwners` al
guardar. El proyecto ya permite guardados parciales/inconsistentes mientras la config está en
`DRAFT` — la consistencia final no se fuerza en este punto del flujo (no hay hoy ningún mecanismo
de validación de completitud al activar una config, y agregar uno está fuera de alcance de esta
historia).

Es un cambio de forma sobre el nodo `rules` ya existente (VPR-9643) — no agrega un nodo nuevo ni
un endpoint nuevo. Sigue viviendo en `PUT /configs/{id}/distribution-rules`.
