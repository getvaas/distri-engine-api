**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9633
**Plan implemented**: —

# Story: Payment Filters — Conciliation Requirements

### Description
Algunos deals no pueden distribuir hasta que ciertas conciliaciones estén al día — por ejemplo, no
distribuir hasta que `payment_tape` esté conciliado contra `payments`. El deal necesita poder
declarar qué pares de tablas son obligatorios (y para qué gateway) antes de que la distribución
pueda correr.

### Acceptance Criteria
- [ ] **Given** al menos un grupo con al menos una regla `{tableA, tableB, gateway}`, **When** se
  guarda, **Then** la config persiste la lista de grupos (OR entre grupos, AND dentro de cada
  grupo) tal cual.
- [ ] **Given** una regla con `gateway` sin especificar, **When** se guarda, **Then** se interpreta
  como "aplica a todos los gateways del deal".
- [ ] **Given** una regla con `tableA` igual a `tableB`, **When** se intenta guardar, **Then** se
  rechaza — no tiene sentido reconciliar una tabla contra sí misma.
- [ ] **Given** un grupo sin ninguna regla, **When** se intenta guardar, **Then** se rechaza.
- [ ] **Given** ningún requerimiento configurado (lista de grupos vacía), **When** se guarda,
  **Then** se persiste vacío sin error — un deal puede no tener ningún requisito de conciliación.

### Additional Context
Las 5 tablas del builder son `PAYMENT_TAPE`, `PAYMENTS`, `FUNDS_TRANSFER`, `DISBURSEMENTS`,
`BORROWER_CORE`. Es un requisito booleano ("esta conciliación es requerida"), no un gate de
tolerancia — el % de tolerancia (épica E8) no vive en esta historia, queda pendiente confirmar en
qué etapa vive (candidato: Readiness Checks). Riesgo abierto, documentado y no resuelto aquí: el
motor real de conciliación (`ConciliationType` en `master-trust-servicer-api`) solo distingue 3
conceptos (`payment_tape`, `payments`, `borrower_core`) — `Funds Transfer` y `Disbursements` no
tienen representación propia, quedan implícitos dentro de un chequeo genérico "Payments vs Bank".
El wizard igual ofrece las 5 opciones del mockup; cómo se verifica cada combinación contra el
motor real se resuelve cuando se implemente el consumo real en Readiness Checks, no aquí.
