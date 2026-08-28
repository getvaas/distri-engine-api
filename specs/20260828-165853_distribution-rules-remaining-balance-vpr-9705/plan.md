**Created at**: 2026-08-28
**Status**: Approved
**Based on story**: @specs/20260828-165853_distribution-rules-remaining-balance-vpr-9705/story.md

# Plan: Definir destino del remanente tras aplicar todas las reglas de distribución

### Goal
Agregar `remainingBalance` a `DistributionRulesConfig` para que el deal declare, opcionalmente, a
qué componente de cuota se atribuye el remanente sin asignar y a qué cuenta destino se transfiere,
una vez aplicadas todas las reglas anteriores de la cascada.

### Context
- `src/main/java/.../domain/model/DistributionRulesConfig.java` — modelo actual a extender.
- `src/main/java/.../infrastructure/web/dto/UpdateDistributionRulesRequest.java` — DTO actual a
  extender.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` —
  `buildDistributionRulesConfig` mapea el campo nuevo; sin validación cruzada.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.
- Rama base: `feature/VPR-9704-deductions` (cadena completa de Distribution Rules implementada,
  sin mergear).

### Public Contracts
- **Domain**: `RemainingBalanceConfig(PaymentComponent component, Long destinationAccountId)`
  (nuevo). `DistributionRulesConfig` gana `remainingBalance: RemainingBalanceConfig` (opcional).
- **DTO**: `RemainingBalanceConfigRequest(PaymentComponent component, Long destinationAccountId)`
  (nuevo). `UpdateDistributionRulesRequest` gana `remainingBalance: RemainingBalanceConfigRequest`.
- **Services**: `UpdateDistributionRulesUseCase.buildDistributionRulesConfig` mapea
  `RemainingBalanceConfigRequest` → `RemainingBalanceConfig` (null-safe), sin validación cruzada
  nueva.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_remainingBalanceWithAllFields_persists`
  - `execute_remainingBalanceNotSent_persistsAsNull`
  - `execute_remainingBalanceWithOnlyOneField_persistsWithoutError`

### Phases

#### Phase 1: Modelo + DTO
- [ ] `RemainingBalanceConfig` (domain, nuevo)
- [ ] `DistributionRulesConfig` gana `remainingBalance: RemainingBalanceConfig`
- [ ] `RemainingBalanceConfigRequest` (DTO, nuevo)
- [ ] `UpdateDistributionRulesRequest` gana `remainingBalance: RemainingBalanceConfigRequest`

#### Phase 2: Use case
- [ ] `buildDistributionRulesConfig` mapea `remainingBalance` (null-safe: `null` si no viene)
- [ ] Sin validación cruzada entre `component` y `destinationAccountId`

#### Phase 3: Tests + Docs
- [ ] `execute_remainingBalanceWithAllFields_persists`
- [ ] `execute_remainingBalanceNotSent_persistsAsNull`
- [ ] `execute_remainingBalanceWithOnlyOneField_persistsWithoutError`
- [ ] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `remainingBalance`

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con los campos nuevos antes
de tocar el use case.
