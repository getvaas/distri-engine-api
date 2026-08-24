**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9634
**Plan implemented**: —

# Story: Payment Filters — Date & Time Filters

### Description
Algunos deals necesitan un corte de fecha para decidir qué pagos entran en la distribución —
"solo pagos anteriores a hoy" o "no considerar pagos con más de N días de antigüedad", a veces con
excepciones puntuales por gateway (ej. JTP: EFECTY liquida bisemanal, PSE con un día extra de
rezago). El deal necesita poder declarar estas reglas de corte, por gateway o para todos.

### Acceptance Criteria
- [ ] **Given** una regla `Distribute by date`/`Distribute by date & time` con `operator` e
  `value`, **When** se guarda, **Then** la config persiste el gateway, el tipo de regla, el
  operador y el valor tal cual.
- [ ] **Given** una regla `Days back limit` con `maxDays`, **When** se guarda, **Then** la config
  persiste el gateway, el tipo de regla y `maxDays` tal cual.
- [ ] **Given** una regla `Distribute by date`/`Distribute by date & time` sin `operator` o sin
  `value`, **When** se intenta guardar, **Then** se rechaza.
- [ ] **Given** una regla `Days back limit` sin `maxDays` (o con `maxDays` ≤ 0), **When** se
  intenta guardar, **Then** se rechaza.
- [ ] **Given** una regla sin `gateway` especificado, **When** se guarda, **Then** se interpreta
  como "aplica a todos los gateways del deal".
- [ ] **Given** ninguna regla configurada (lista vacía), **When** se guarda, **Then** se persiste
  vacío sin error.

### Additional Context
A diferencia de Accounting Payments (VPR-9631) y Conciliation Requirements (VPR-9633), esta card
no tiene AND/OR — es una lista plana de reglas independientes (confirmado contra el mockup y
`docs/distribution-engine-onboarding.html`: solo `+ Add filter rule`). `Distribute by date` y
`Distribute by date & time` son el mismo mecanismo de comparación (`Is before`/`Is after`), solo
cambia la granularidad (día vs. fecha+hora exacta) — no hay comportamiento adicional distinto
entre ambos, así que comparten el mismo tipo de regla con un flag de granularidad, no dos enums de
operador separados. El valor acepta el keyword `"today"` o una fecha absoluta ISO — sin
expresiones relativas por ahora. El "Days back" global de Pool Strategy (VPR-9628, default 90) es
el mismo mecanismo que "Days back limit" aquí — esta etapa permite una excepción más fina por
gateway sobre ese default global; esta historia no reimplementa esa relación en código (queda para
la etapa de ejecución), solo tipa la estructura de la regla.
