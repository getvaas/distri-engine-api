**Created at**: 2026-08-28
**Status**: Done
**Based on story**: @specs/20260828-154741_distribution-rules-account-transfer-rules-vpr-9702/story.md

# Plan: Definir reglas de transferencia entre cuentas por balance strategy

### Goal
Agregar `accountTransferRules` a `BalanceStrategyConfig` para que cada balance strategy declare,
como lista, las combinaciones de cuentas origen/destino y su condición opcional que rigen el
movimiento de balance de un owner.

### Context
- `src/main/java/.../domain/model/BalanceStrategyConfig.java` — modelo actual a extender (VPR-9703).
- `src/main/java/.../infrastructure/web/dto/BalanceStrategyConfigRequest.java` — DTO actual a
  extender.
- `src/main/java/.../domain/model/PaymentFilterCondition.java` /
  `infrastructure/web/dto/PaymentFilterConditionRequest.java` — reusar directamente, ya existen
  (VPR-9631), no se crea modelo de condición nuevo.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` —
  `buildBalanceStrategyConfig` mapea la lista nueva; sin validación cruzada.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.
- Rama base: `feature/VPR-9703-balance-strategy` (BalanceStrategyConfig ya implementado, sin
  mergear).

### Public Contracts
- **Domain**: `AccountTransferRule(List<Long> fromAccountIds, List<Long> toAccountIds, PaymentFilterCondition condition)`
  (nuevo). `BalanceStrategyConfig` gana `accountTransferRules: List<AccountTransferRule>`.
- **DTO**: `AccountTransferRuleRequest(List<Long> fromAccountIds, List<Long> toAccountIds, PaymentFilterConditionRequest condition)`
  (nuevo, reusa `PaymentFilterConditionRequest` existente). `BalanceStrategyConfigRequest` gana
  `accountTransferRules: List<AccountTransferRuleRequest>`.
- **Services**: `UpdateDistributionRulesUseCase.buildBalanceStrategyConfig` mapea
  `AccountTransferRuleRequest` → `AccountTransferRule` (incluyendo `condition`), sin validación
  cruzada entre `fromAccountIds`/`toAccountIds`.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_accountTransferRuleWithCondition_persists`
  - `execute_accountTransferRuleWithoutCondition_persistsAsNull`
  - `execute_emptyOrMissingAccountTransferRules_persistsEmptyList`
  - `execute_sameAccountIdInFromAndTo_persistsWithoutError`

### Phases

#### Phase 1: Modelo + DTO
- [x] `AccountTransferRule` (domain, nuevo)
- [x] `BalanceStrategyConfig` gana `accountTransferRules: List<AccountTransferRule>`
- [x] `AccountTransferRuleRequest` (DTO, nuevo, reusa `PaymentFilterConditionRequest`)
- [x] `BalanceStrategyConfigRequest` gana `accountTransferRules: List<AccountTransferRuleRequest>`

#### Phase 2: Use case
- [x] `buildBalanceStrategyConfig` mapea `accountTransferRules` (null-safe: lista vacía si no
      viene)
- [x] Mapeo de `condition` (`PaymentFilterConditionRequest` → `PaymentFilterCondition`), `null`
      si no viene
- [x] Sin validación cruzada entre `fromAccountIds` y `toAccountIds`

#### Phase 3: Tests + Docs
- [x] `execute_accountTransferRuleWithCondition_persists`
- [x] `execute_accountTransferRuleWithoutCondition_persistsAsNull`
- [x] `execute_emptyOrMissingAccountTransferRules_persistsEmptyList`
- [x] `execute_sameAccountIdInFromAndTo_persistsWithoutError`
- [x] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `accountTransferRules`

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con los campos nuevos antes
de tocar el use case.
