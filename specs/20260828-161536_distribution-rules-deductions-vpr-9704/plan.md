**Created at**: 2026-08-28
**Status**: Done
**Based on story**: @specs/20260828-161536_distribution-rules-deductions-vpr-9704/story.md

# Plan: Definir deducciones de comisión por regla de transferencia de cuenta

### Goal
Agregar `deductions` a `AccountTransferRule` para que cada regla de transferencia declare, como
lista, las comisiones a descontar (concepto, tipo, valor, cuenta destinataria opcional y
periodicidad).

### Context
- `src/main/java/.../domain/model/AccountTransferRule.java` — modelo actual a extender (VPR-9702).
- `src/main/java/.../infrastructure/web/dto/AccountTransferRuleRequest.java` — DTO actual a
  extender.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` —
  `buildAccountTransferRules` mapea la lista nueva; sin validación cruzada.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.
- Rama base: `feature/VPR-9702-account-transfer-rules` (`AccountTransferRule` ya implementado,
  sin mergear).

### Public Contracts
- **Domain**:
  - `DeductionType` (enum): `PERCENTAGE`, `FIXED`.
  - `DeductionPeriodicity` (enum): `ALWAYS`, `ONCE_PER_DISTRIBUTION`, `ONCE_PER_MONTH`,
    `ONCE_PER_WEEK`.
  - `Deduction(String concept, DeductionType type, BigDecimal value, Long accountId, DeductionPeriodicity periodicity)`
    (nuevo).
  - `AccountTransferRule` gana `deductions: List<Deduction>`.
- **DTO**:
  - `DeductionRequest(String concept, DeductionType type, BigDecimal value, Long accountId, DeductionPeriodicity periodicity)`
    (nuevo).
  - `AccountTransferRuleRequest` gana `deductions: List<DeductionRequest>`.
- **Services**: `UpdateDistributionRulesUseCase.buildAccountTransferRules` mapea
  `DeductionRequest` → `Deduction` dentro de cada regla, sin validación cruzada nueva.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_deductionWithAllFields_persists`
  - `execute_deductionWithNullAccountId_persistsAsIs`
  - `execute_emptyOrMissingDeductions_persistsEmptyList`
  - `execute_multipleDeductions_persistsAll`

### Phases

#### Phase 1: Modelo + DTO
- [x] `DeductionType`, `DeductionPeriodicity` (enums, `domain/model/enums/`)
- [x] `Deduction` (domain, nuevo)
- [x] `AccountTransferRule` gana `deductions: List<Deduction>`
- [x] `DeductionRequest` (DTO, nuevo)
- [x] `AccountTransferRuleRequest` gana `deductions: List<DeductionRequest>`

#### Phase 2: Use case
- [x] `buildAccountTransferRules` mapea `deductions` (null-safe: lista vacía si no viene)
- [x] Sin validación cruzada ni de completitud sobre los campos de `Deduction`

#### Phase 3: Tests + Docs
- [x] `execute_deductionWithAllFields_persists`
- [x] `execute_deductionWithNullAccountId_persistsAsIs`
- [x] `execute_emptyOrMissingDeductions_persistsEmptyList`
- [x] `execute_multipleDeductions_persistsAll`
- [x] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `deductions`

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con los campos nuevos antes
de tocar el use case.
