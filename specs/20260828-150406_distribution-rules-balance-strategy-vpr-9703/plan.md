**Created at**: 2026-08-28
**Status**: Done
**Based on story**: @specs/20260828-150406_distribution-rules-balance-strategy-vpr-9703/story.md

# Plan: Definir estrategia de balance por regla de owner de componente

### Goal
Agregar `balanceStrategy` a `ComponentOwnerRule` para que cada regla owner/componente declare, de
forma opcional, la columna de monto a usar, la estrategia de suficiencia de balance y la
estrategia de distribución del monto (con su parámetro numérico), sin validación cruzada entre
esos dos últimos campos.

### Context
- `src/main/java/.../domain/model/ComponentOwnerRule.java` — modelo actual a extender.
- `src/main/java/.../infrastructure/web/dto/ComponentOwnerRuleRequest.java` — DTO actual a
  extender.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` —
  `buildComponentOwnerRule` mapea el nuevo campo; las validaciones existentes (component/owner
  obligatorios, sin duplicados de component) no cambian.
- `src/main/java/.../domain/model/PaymentTapePoolConfig.java` — patrón ya establecido para
  `amountField` (string libre, VPR-9628), a replicar acá.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.
- Rama base: `master` (sin VPR-9699 todavía — rama hermana independiente).

### Public Contracts
- **Domain**:
  - `BalanceSufficiencyStrategy` (enum): `SUFFICIENT_OR_STOP`, `UNTIL_EXHAUSTED`,
    `SKIP_IF_INSUFFICIENT`, `IGNORE_BALANCE`.
  - `AmountDistributionStrategy` (enum): `DEFAULT`, `PROPORTIONAL_WEIGHT`, `PERCENTAGE_OF_POOL`,
    `PERCENTAGE_OF_REMAINING`, `FIXED_AMOUNT`.
  - `BalanceStrategyConfig(String amountField, BalanceSufficiencyStrategy sufficiencyStrategy, AmountDistributionStrategy distributionStrategy, BigDecimal distributionValue)`
    (nuevo).
  - `ComponentOwnerRule` gana `balanceStrategy: BalanceStrategyConfig` (opcional, último
    parámetro).
- **DTO**:
  - `BalanceStrategyConfigRequest(String amountField, BalanceSufficiencyStrategy sufficiencyStrategy, AmountDistributionStrategy distributionStrategy, BigDecimal distributionValue)`
    (nuevo).
  - `ComponentOwnerRuleRequest` gana `balanceStrategy: BalanceStrategyConfigRequest`.
- **Services**: `UpdateDistributionRulesUseCase.buildComponentOwnerRule` mapea
  `BalanceStrategyConfigRequest` → `BalanceStrategyConfig` (mapeo directo, `null` si no viene) sin
  validación cruzada nueva.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_ruleWithBalanceStrategy_persists`
  - `execute_ruleWithoutBalanceStrategy_persistsAsNull`
  - `execute_fixedAmountWithoutDistributionValue_persistsWithoutError`
  - `execute_defaultWithDistributionValueSet_persistsAsIs`

### Phases

#### Phase 1: Modelo + DTO
- [x] `BalanceSufficiencyStrategy`, `AmountDistributionStrategy` (enums, `domain/model/enums/`)
- [x] `BalanceStrategyConfig` (domain, nuevo)
- [x] `ComponentOwnerRule` gana `balanceStrategy: BalanceStrategyConfig`
- [x] `BalanceStrategyConfigRequest` (DTO, nuevo)
- [x] `ComponentOwnerRuleRequest` gana `balanceStrategy: BalanceStrategyConfigRequest`

#### Phase 2: Use case
- [x] `buildComponentOwnerRule` mapea `balanceStrategy` (null-safe, sin validación cruzada entre
      `distributionStrategy` y `distributionValue`)
- [x] Validaciones existentes de `ComponentOwnerRule` (component/owner obligatorios, sin
      duplicados) sin cambios

#### Phase 3: Tests + Docs
- [x] `execute_ruleWithBalanceStrategy_persists`
- [x] `execute_ruleWithoutBalanceStrategy_persistsAsNull`
- [x] `execute_fixedAmountWithoutDistributionValue_persistsWithoutError`
- [x] `execute_defaultWithDistributionValueSet_persistsAsIs`
- [x] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `balanceStrategy` y los 2 enums

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con los campos nuevos antes
de tocar el use case.
