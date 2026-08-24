**Created at**: 2026-08-21
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9662
**Plan implemented**: @specs/20260821-155208_payment-tape-search-window-working-days-vpr-9662/plan.md

# Story: Ventana de búsqueda de payment tapes candidatos

### Description
Antes de armar cualquier assignment, el motor tiene que traer el conjunto de payment tapes candidatos:
los que todavía no se distribuyeron, dentro de una ventana de días hacia atrás desde la fecha de la
corrida. Esa ventana no es un número de días calendario plano — se ajusta por días hábiles del país del
borrower (Deal Info), y el número de días viene de Pool Strategy (`daysBack`, VPR-9628). Es el primer
punto donde el motor de ejecución toca datos reales de `payment_tape` — hasta ahora solo tocamos nuestra
propia tabla de configuración.

### Acceptance Criteria
- [x] **Given** una config activa con `daysBack=90` y país Colombia, **When** se pide el pool candidato
  para una fecha, **Then** la ventana de búsqueda retrocede 90 días hábiles (no 90 días calendario)
  desde esa fecha.
- [x] **Given** payment tapes dentro de la ventana con `distributionId=null`, **When** se pide el pool
  candidato, **Then** se incluyen.
- [x] **Given** payment tapes dentro de la ventana pero ya con `distributionId` seteado, **When** se
  pide el pool candidato, **Then** se excluyen — nunca se vuelve a considerar un pago ya distribuido.
- [x] **Given** payment tapes fuera de la ventana (más viejos que `daysBack` días hábiles), **When** se
  pide el pool candidato, **Then** se excluyen.
- [x] **Given** un `companyId` sin ninguna config activa, **When** se pide el pool candidato, **Then**
  falla igual que ya falla `ResolveActiveDistributionConfigUseCase` (VPR-9660) — no se duplica esa
  validación.

### Additional Context
Esta historia **no** aplica todavía los filtros configurables (Accounting Payments, Gateway, Date&Time —
VPR-9631/9632/9634) ni el gate de conciliación (VPR-9633) — esos se layer-ean encima del resultado de
esta historia en tickets siguientes. Tampoco escribe nada en `payment_tape`: es 100% lectura, la tabla la
mantiene `payment-data-extractor`.
