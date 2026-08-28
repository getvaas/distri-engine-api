**Created at**: 2026-08-28
**Status**: Approved
**Based on story**: @specs/20260828-172628_distribution-rules-accounting-payments-per-component-vpr-9706/story.md

# Plan: Habilitar distribución de pagos contables por componente de cuota

### Goal
Agregar `distributeAccountingPayments` a `ComponentOwnerRule` para que cada regla owner+componente
declare, de forma independiente al flag del deal (VPR-9631), si distribuye pagos contables o no.

### Context
- `src/main/java/.../domain/model/ComponentOwnerRule.java` — modelo actual a extender.
- `src/main/java/.../infrastructure/web/dto/ComponentOwnerRuleRequest.java` — DTO actual a
  extender.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` —
  `buildComponentOwnerRule` mapea el campo nuevo; mismo patrón `Boolean.TRUE.equals(...)` que
  `hasComponentOwners` (VPR-9699) para el default.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.
- Rama base: `master` actualizado (VPR-9699 a VPR-9705 ya mergeados).

### Public Contracts
- **Domain**: `ComponentOwnerRule` gana `distributeAccountingPayments: boolean` (nuevo, 5to
  parámetro).
- **DTO**: `ComponentOwnerRuleRequest` gana `distributeAccountingPayments: Boolean`.
- **Services**: `UpdateDistributionRulesUseCase.buildComponentOwnerRule` mapea el campo con
  `Boolean.TRUE.equals(...)`, default `false` si no viene.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_distributeAccountingPaymentsTrue_persists`
  - `execute_distributeAccountingPaymentsFalse_persists`
  - `execute_distributeAccountingPaymentsNotSent_defaultsToFalse`

### Phases

#### Phase 1: Modelo + DTO
- [ ] `ComponentOwnerRule` gana `distributeAccountingPayments: boolean`
- [ ] `ComponentOwnerRuleRequest` gana `distributeAccountingPayments: Boolean`

#### Phase 2: Use case
- [ ] `buildComponentOwnerRule` mapea `distributeAccountingPayments` con
      `Boolean.TRUE.equals(...)`, default `false`

#### Phase 3: Tests + Docs
- [ ] `execute_distributeAccountingPaymentsTrue_persists`
- [ ] `execute_distributeAccountingPaymentsFalse_persists`
- [ ] `execute_distributeAccountingPaymentsNotSent_defaultsToFalse`
- [ ] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `distributeAccountingPayments`

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con el campo nuevo antes de
tocar el use case.
