**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-104109_payment-filters-date-time-filters-vpr-9634/story.md

# Plan: Payment Filters — Date & Time Filters

### Goal
Agregar el campo `dateTimeFilters` a `DistributablePaymentsConfig` (nuevo, no reemplaza ningún
placeholder) y extender `UpdateDistributablePaymentsUseCase` para persistirlo, sin duplicar el
endpoint `PUT /configs/{id}/payment-filters`.

### Context
- `DistributablePaymentsConfig.java` (VPR-9631/9632/9633) — se agrega `dateTimeFilters`, mismo
  nivel que las 3 secciones ya tipadas.
- A diferencia de `AccountingPaymentsConfig`/`ConciliationRequirementsConfig`, esta sección es una
  lista plana (sin grupos AND/OR) — confirmado contra el mockup y el HTML fuente.
- `operator` (`IS_BEFORE`/`IS_AFTER`) solo aplica a `DISTRIBUTE_BY_DATE`/`DISTRIBUTE_BY_DATE_TIME`;
  `maxDays` solo aplica a `DAYS_BACK_LIMIT` — un solo record con campos nullable según `ruleType`,
  siguiendo el mismo estilo que el mockup (una fila, columnas condicionales).

### Public Contracts
- **Domain**: `DateTimeFilterRuleType` (enum: `DISTRIBUTE_BY_DATE`, `DISTRIBUTE_BY_DATE_TIME`,
  `DAYS_BACK_LIMIT`), `DateTimeFilterOperator` (enum: `IS_BEFORE`, `IS_AFTER`),
  `DateTimeFilterRule(gateway, ruleType, operator, value, maxDays)`,
  `DateTimeFiltersConfig(rules)`.
- **DTO**: `DateTimeFilterRuleRequest`; `UpdatePaymentFiltersRequest` extendido con
  `dateTimeFilters`.
- **Endpoint**: sin cambios — `PUT /configs/{id}/payment-filters` (ya existe).
- **Tests**: extender `UpdateDistributablePaymentsUseCaseTest` con los casos de Date & Time
  Filters.

### Phases

#### Phase 1: Modelo tipado + use case
[Agrega el campo nuevo y extiende el use case existente — no crea un endpoint nuevo.]
- [ ] `DateTimeFilterRuleType`, `DateTimeFilterOperator`, `DateTimeFilterRule`,
  `DateTimeFiltersConfig`
- [ ] `DistributablePaymentsConfig.dateTimeFilters` (nuevo campo)
- [ ] `DateTimeFilterRuleRequest` (DTO) + `UpdatePaymentFiltersRequest.dateTimeFilters`
- [ ] `UpdateDistributablePaymentsUseCase`: reglas no enviadas/vacías → persiste lista vacía sin
  error; `ruleType` requerido; si `DISTRIBUTE_BY_DATE`/`DISTRIBUTE_BY_DATE_TIME` → requiere
  `operator` + `value` no vacío; si `DAYS_BACK_LIMIT` → requiere `maxDays` > 0; `gateway` nulo se
  persiste tal cual (significa "All gateways")

#### Phase 2: Tests
[Cubre la lista vacía válida, las validaciones nuevas, y que no rompe los tests existentes.]
- [ ] Regla `DISTRIBUTE_BY_DATE`/`DATE_TIME` con `operator`+`value` → persiste tal cual
- [ ] Regla `DAYS_BACK_LIMIT` con `maxDays` → persiste tal cual
- [ ] Regla `DISTRIBUTE_BY_DATE`/`DATE_TIME` sin `operator` o `value` → error
- [ ] Regla `DAYS_BACK_LIMIT` sin `maxDays` o con `maxDays` ≤ 0 → error
- [ ] Regla con `gateway=null` → persiste como "All gateways"
- [ ] `dateTimeFilters` no enviado / vacío → persiste lista vacía, sin error

### Next Step
Payment Filters (Etapa 3) queda completo: Accounting Payments (VPR-9631), Gateway Filters
(VPR-9632), Conciliation Requirements (VPR-9633) y Date & Time Filters (VPR-9634). Siguiente etapa
del wizard: Virtual Columns (Etapa 4, aún sin estructura definida) o Distribution Rules (Etapa 5,
VPR-9643) — a decidir con el usuario cuál seguir primero.
