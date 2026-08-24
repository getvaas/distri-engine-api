**Created at**: 2026-08-21
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9631
**Plan implemented**: —

# Story: Payment Filters — Accounting Payments

### Description
Algunos deals reciben pagos contables (sin respaldo de caja real) mezclados en el payment tape —
por ejemplo siniestros o write-offs. El deal necesita poder identificar esos pagos mediante
condiciones configurables sobre las columnas del payment tape, y decidir si de todas formas se
distribuyen o se excluyen del pool.

### Acceptance Criteria
- [ ] **Given** el toggle "has accounting payments" activo con al menos un grupo de condiciones,
  **When** se guarda, **Then** la config persiste la lista de grupos (OR entre grupos, AND dentro de
  cada grupo) con campo, operador y valor de cada condición.
- [ ] **Given** el toggle "has accounting payments" activo sin ninguna condición, **When** se intenta
  guardar, **Then** se rechaza — no tiene sentido activarlo sin definir cómo identificar los pagos.
- [ ] **Given** una condición con operador `IS_NULL` o `IS_NOT_NULL`, **When** se guarda, **Then** no
  requiere `value` (a diferencia del resto de operadores, que sí lo requieren).
- [ ] **Given** una condición con operador `IN` o `NOT_IN`, **When** se guarda con `value` en formato
  `"Siniestro, write-off"`, **Then** se interpreta como lista de valores separados por coma.
- [ ] **Given** el toggle "has accounting payments" desactivado, **When** se guarda, **Then** el
  toggle "distribute accounting payments" se ignora y no aplica ningún filtro (no hay pagos contables
  que identificar).
- [ ] **Given** "distribute accounting payments" desactivado con condiciones definidas, **When** se
  guarda, **Then** queda registrado que los pagos que matcheen esas condiciones deben excluirse del
  pool (la exclusión real ocurre en la etapa de ejecución, no en esta historia).

### Additional Context
El campo de cada condición es cualquier columna real del payment tape (mismo patrón abierto que
`amountField` en Pool Strategy VPR-9628), no un enum cerrado — el diccionario ya trae `payment_type`
como campo típico para este caso, pero no se restringe a él. La agrupación tiene exactamente 2
niveles: una lista de grupos unidos por OR, cada grupo una lista de condiciones unidas por AND — sin
anidamiento más profundo (así es el mockup, `docs/screen-payments-filters.png`). El matiz de la épica
E5.3b (distribuir según si el owner es lender o borrower) se resuelve como una condición más de este
mismo builder sobre un campo `owner`, si ese campo existe en el payment tape del deal — no hace falta
un mecanismo nuevo ni tocar Distribution Rules. Gateway Filters es una card separada del mockup y un
ticket distinto (VPR-9632) — no se toca en esta historia.
