**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-102249_payment-filters-conciliation-requirements-vpr-9633/story.md

# Plan: Payment Filters — Conciliation Requirements

### Goal
Agregar el campo `conciliationRequirements` a `DistributablePaymentsConfig` (nuevo, no reemplaza
ningún placeholder) y extender `UpdateDistributablePaymentsUseCase` para persistirlo, sin duplicar
el endpoint `PUT /configs/{id}/payment-filters`.

### Context
- `DistributablePaymentsConfig.java` (VPR-9631/9632) — se agrega `conciliationRequirements`, mismo
  nivel que `accountingPayments` y `gatewayFilters`.
- Mismo patrón de agrupación AND/OR de 2 niveles que `AccountingPaymentsConfig` (VPR-9631):
  `PaymentFilterConditionGroup` ya existe para condiciones sueltas — pero una regla de conciliación
  no es una condición de campo/operador/valor, es un par de tablas + gateway, así que necesita su
  propio tipo de "regla" y su propio grupo (no reutiliza `PaymentFilterConditionGroup`).
- Riesgo abierto (no se resuelve aquí): el motor real de conciliación no distingue Funds
  Transfer/Disbursements — ver `story.md`.

### Public Contracts
- **Domain**: `ConciliationTable` (enum: `PAYMENT_TAPE`, `PAYMENTS`, `FUNDS_TRANSFER`,
  `DISBURSEMENTS`, `BORROWER_CORE`), `ConciliationRequirementRule(tableA, tableB, gateway)`,
  `ConciliationRequirementGroup(rules)`, `ConciliationRequirementsConfig(groups)`.
- **DTO**: `ConciliationRequirementRuleRequest`, `ConciliationRequirementGroupRequest`;
  `UpdatePaymentFiltersRequest` extendido con `conciliationRequirements`.
- **Endpoint**: sin cambios — `PUT /configs/{id}/payment-filters` (ya existe).
- **Tests**: extender `UpdateDistributablePaymentsUseCaseTest` con los casos de Conciliation
  Requirements.

### Phases

#### Phase 1: Modelo tipado + use case
[Agrega el campo nuevo y extiende el use case existente — no crea un endpoint nuevo.]
- [ ] `ConciliationTable`, `ConciliationRequirementRule`, `ConciliationRequirementGroup`,
  `ConciliationRequirementsConfig`
- [ ] `DistributablePaymentsConfig.conciliationRequirements` (nuevo campo)
- [ ] `ConciliationRequirementRuleRequest`, `ConciliationRequirementGroupRequest` (DTOs) +
  `UpdatePaymentFiltersRequest.conciliationRequirements`
- [ ] `UpdateDistributablePaymentsUseCase`: grupos vacíos/no enviados → persiste lista vacía sin
  error; cada grupo requiere ≥1 regla; cada regla requiere `tableA`/`tableB` no nulos y distintos
  entre sí; `gateway` nulo se persiste tal cual (significa "All gateways")

#### Phase 2: Tests
[Cubre la lista vacía válida, las 2 validaciones nuevas, y que no rompe los tests existentes.]
- [ ] Grupo con regla válida → persiste tal cual
- [ ] Regla con `gateway=null` → persiste como "All gateways" (no se fuerza ningún valor)
- [ ] Regla con `tableA == tableB` → error
- [ ] Grupo sin reglas → error
- [ ] `conciliationRequirements` no enviado / vacío → persiste lista vacía, sin error

### Next Step
Date & Time Filters (VPR-9634) agrega otro campo más a `DistributablePaymentsConfig` — mismo
endpoint, mismo patrón.
