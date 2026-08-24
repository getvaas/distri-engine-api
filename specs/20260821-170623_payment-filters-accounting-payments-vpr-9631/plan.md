**Created at**: 2026-08-21
**Status**: Approved
**Based on story**: @specs/20260821-170623_payment-filters-accounting-payments-vpr-9631/story.md

# Plan: Payment Filters — Accounting Payments

### Goal
Tipar la sección `distributablePayments` del payload (hoy `JsonNode` placeholder) para su parte de
Accounting Payments, con un nuevo endpoint dedicado a la etapa "Payment Filters" del wizard.

### Context
- `DistributionConfigPayload.java` — `distributablePayments` pasa de `JsonNode` a
  `DistributablePaymentsConfig`. El campo `gatewayFilters` dentro de esa clase queda como `JsonNode`
  placeholder hasta VPR-9632 (mismo patrón que `dataSourceAggregation` en `PoolConfig`).
- Mismo patrón arquitectónico que `UpdatePoolConfigUseCase` (VPR-9628/9629): un único endpoint por
  etapa del wizard, use case que lee la config existente, aplica el cambio a su sección y preserva el
  resto del payload intacto.
- `value` de cada condición se persiste como `String` crudo (ej. `"Siniestro, write-off"`) — el
  parseo a lista para `IN`/`NOT_IN` es responsabilidad de la etapa de ejecución, no de esta historia.

### Public Contracts
- **Domain**: `PaymentFilterOperator` (enum: `EQ`, `NEQ`, `IN`, `NOT_IN`, `IS_NULL`, `IS_NOT_NULL`,
  `CONTAINS`, `GT`, `LT`), `PaymentFilterCondition(field, operator, value)`,
  `PaymentFilterConditionGroup(conditions)`,
  `AccountingPaymentsConfig(hasAccountingPayments, distributeAccountingPayments, conditionGroups)`,
  `DistributablePaymentsConfig(accountingPayments, gatewayFilters)`.
- **DTO**: `PaymentFilterConditionRequest`, `PaymentFilterConditionGroupRequest`,
  `AccountingPaymentsRequest`, `UpdatePaymentFiltersRequest(accountingPayments)`.
- **Endpoint**: `PUT /configs/{id}/payment-filters` (nuevo).
- **Use case**: `UpdateDistributablePaymentsUseCase`.
- **Tests**: `UpdateDistributablePaymentsUseCaseTest` (nuevo).

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de distributablePayments.]
- [ ] `PaymentFilterOperator`, `PaymentFilterCondition`, `PaymentFilterConditionGroup`,
  `AccountingPaymentsConfig`, `DistributablePaymentsConfig`
- [ ] `DistributionConfigPayload.distributablePayments` de `JsonNode` a `DistributablePaymentsConfig`
- [ ] `PaymentFilterConditionRequest`, `PaymentFilterConditionGroupRequest`,
  `AccountingPaymentsRequest`, `UpdatePaymentFiltersRequest` (DTOs)

#### Phase 2: Use case + validaciones
[Nuevo use case — no reutiliza UpdatePoolConfigUseCase, es otra etapa del wizard.]
- [ ] `hasAccountingPayments` default `false`
- [ ] Si `hasAccountingPayments=false`: fuerza `distributeAccountingPayments=true` y
  `conditionGroups=[]`, ignorando lo que venga en el request
- [ ] Si `hasAccountingPayments=true`: requiere ≥1 grupo, cada grupo con ≥1 condición
- [ ] Validación por condición: `field` y `operator` requeridos; `value` requerido salvo en
  `IS_NULL`/`IS_NOT_NULL`
- [ ] Preserva `gatewayFilters` (JsonNode) y el resto del payload sin tocar

#### Phase 3: Endpoint
[Router — mismo patrón que PUT /configs/{id}/pool.]
- [ ] `PUT /configs/{id}/payment-filters` en `DistributionConfigRouter`

#### Phase 4: Tests
[Cubre defaults, las 2 ramas de hasAccountingPayments, y las validaciones de condición.]
- [ ] `hasAccountingPayments=true` con grupos válidos → persiste tal cual
- [ ] `hasAccountingPayments=true` sin grupos → error
- [ ] Grupo vacío (sin condiciones) → error
- [ ] Condición sin `field` u `operator` → error
- [ ] Condición `IS_NULL`/`IS_NOT_NULL` sin `value` → válido (no requiere value)
- [ ] Condición con operador que no sea `IS_NULL`/`IS_NOT_NULL` sin `value` → error
- [ ] `hasAccountingPayments=false` → fuerza `distributeAccountingPayments=true` y `conditionGroups=[]`
  aunque el request traiga otra cosa
- [ ] Preserva el resto del payload (`pool`, `country`, etc.) sin modificar

### Next Step
Gateway Filters (VPR-9632) tipa el campo `gatewayFilters` de `DistributablePaymentsConfig` y extiende
`UpdatePaymentFiltersRequest` con el campo correspondiente — mismo patrón que VPR-9629 extendió
`UpdatePoolConfigRequest`.
