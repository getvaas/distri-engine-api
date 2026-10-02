**Created at**: 2026-10-01
**Status**: Done
**Based on story**: @specs/20261001-142412_account-type-balance-auto-selection/story.md

# Plan: Derivar el campo de balance a usar del tipo real de cuenta

### Goal
Reemplazar la selección manual de "qué campo de balance usar" por una derivación automática a partir
del `AccountType` real de cada cuenta (`COLLECTION`/`INVESTMENT`/`RESERVE`) y la excepción puntual de
`WELLI_INVESTMENT` (por `accountCode`), tanto en la config persistida como en el único consumidor de
ejecución real hoy (`CheckAccountBalanceSufficiencyUseCase`), documentando el gap de `investmentGains`
sin inventarlo.

### Context
- `src/main/java/.../domain/model/enums/PoolBalanceType.java` — el enum ya existente
  (`CURRENT_BALANCE`/`USABLE_BALANCE`), se mantiene como la representación de "qué campo leer";
  pasa a resolverse automáticamente en vez de elegirse.
- `src/main/java/.../application/usecase/CheckAccountBalanceSufficiencyUseCase.java` — único
  consumidor de ejecución real hoy; `usableBalanceOf()` hardcodea `projected ?: current` para toda
  cuenta por igual, sin distinguir tipo ni excepción WELLI.
- `src/main/java/.../domain/model/BalanceStrategyConfig.java`,
  `infrastructure/web/dto/BalanceStrategyConfigRequest.java`,
  `application/usecase/DistributionRulesConfigBuilder.java` — `accountIdsToCheck: List<Long>` pasa a
  ser una lista de objetos (mismo patrón que `AccountBalanceSource`, no un concepto nuevo).
- `src/main/java/.../domain/model/AccountBalanceSource.java`,
  `infrastructure/web/dto/AccountBalanceSourceRequest.java`,
  `application/usecase/PoolConfigBuilder.java` — ya es una lista de objetos con info propia por
  cuenta (`balanceType` manual); se reemplaza ese campo por `accountType`/`accountCode` (el Pool
  Strategy `ACCOUNT_BALANCE` no tiene resolver de ejecución todavía — este cambio es solo de config,
  por consistencia).
- `docs/architecture/distribution-config-schema.md` — documentar la tabla de mapeo tipo→campo y el
  gap de `investmentGains`.
- Sin configs reales en producción todavía (repo en desarrollo activo) — los cambios de shape en
  `accountIdsToCheck`/`AccountBalanceSource.balanceType` no necesitan shim de compatibilidad hacia
  atrás.

### Public Contracts
- **Domain**:
  - `enum AccountType { COLLECTION, INVESTMENT, RESERVE }` — mismos 3 valores que el sistema real.
  - `record AccountBalanceCheckTarget(Long accountId, AccountType accountType, String accountCode)` —
    reemplaza cada elemento de `accountIdsToCheck`.
  - `ResolveAccountBalanceFieldUseCase.execute(AccountType accountType, String accountCode): PoolBalanceType` —
    `accountCode.equals("WELLI_INVESTMENT")` → `CURRENT_BALANCE`; cualquier otro caso →
    `USABLE_BALANCE`. Si `accountType == INVESTMENT` y no es la excepción, logea (nivel INFO/WARN) el
    gap de `investmentGains` documentado — no lo calcula.
  - `BalanceStrategyConfig.accountIdsToCheck: List<Long>` → `accountChecks: List<AccountBalanceCheckTarget>`.
  - `AccountBalanceSource.balanceType: PoolBalanceType` → `accountType: AccountType`,
    `accountCode: String` (nuevo campo).
- **Services**:
  - `CheckAccountBalanceSufficiencyUseCase.execute(...)` — para cada `AccountBalanceCheckTarget` en
    `accountChecks`, resuelve su `PoolBalanceType` vía `ResolveAccountBalanceFieldUseCase` y lee
    `currentBalance`/`projectedBalance ?: currentBalance` según corresponda (hoy: siempre lo segundo).
- **Web DTOs**:
  - `BalanceStrategyConfigRequest.accountChecks: List<AccountBalanceCheckTargetRequest>` (nuevo DTO,
    reemplaza `accountIdsToCheck`).
  - `AccountBalanceSourceRequest.accountType`/`accountCode` reemplaza `balanceType`.
- **Tests**: `ResolveAccountBalanceFieldUseCaseTest` (mapeo por tipo, excepción WELLI, gap
  investmentGains logueado), `CheckAccountBalanceSufficiencyUseCaseTest` actualizado (regresión
  COLLECTION/RESERVE/INVESTMENT-no-WELLI → usableBalance; WELLI → currentBalance),
  `PoolConfigBuilderTest`/`DistributionRulesConfigBuilderTest` actualizados al nuevo shape.

### Phases

#### Phase 1: AccountType + resolver de campo de balance
- [x] `AccountType` enum.
- [x] `ResolveAccountBalanceFieldUseCase` — mapeo tipo/código → `PoolBalanceType`, logging del gap de
      `investmentGains` cuando `accountType == INVESTMENT` y no es la excepción WELLI.
- [x] Tests (6): COLLECTION, RESERVE, INVESTMENT no-WELLI con gap logueado, WELLI por código sin
      importar el tipo (incluye tipo no-INVESTMENT), `accountCode=null`. `./gradlew test` en verde.

#### Phase 2: Balance Sufficiency Check (el consumidor de ejecución real)
- [x] `AccountBalanceCheckTarget` domain record.
- [x] `BalanceStrategyConfig.accountChecks` reemplaza `accountIdsToCheck`.
- [x] `BalanceStrategyConfigRequest`/`AccountBalanceCheckTargetRequest` (DTOs) +
      `DistributionRulesConfigBuilder` actualizado.
- [x] `CheckAccountBalanceSufficiencyUseCase` usa `ResolveAccountBalanceFieldUseCase` por cuenta en
      vez del hardcode actual.
- [x] `CheckAccountBalanceSufficiencyUseCaseTest` nuevo (6 tests: COLLECTION usableBalance, WELLI
      fuerza currentBalance, mezcla de ambas en un mismo chequeo — cada una resuelve su propio
      campo —, INVESTMENT no-WELLI igual a COLLECTION, sin `accountChecks` falla explícito, cuenta
      sin balance registrado). `CalculateAssignmentsUseCaseTest` actualizado al nuevo shape.
      `./gradlew test` en verde.

#### Phase 3: Pool Strategy ACCOUNT_BALANCE (config, sin resolver de ejecución)
- [x] `AccountBalanceSource`/`AccountBalanceSourceRequest` reemplazan `balanceType` por
      `accountType`/`accountCode`.
- [x] `PoolConfigBuilder.buildAccountBalanceConfig` actualizado (sin default de `balanceType`, ya no
      aplica — `accountType` es obligatorio, llega resuelto por el caller).
- [x] `PoolConfigBuilderTest` actualizado al nuevo shape. `./gradlew test` completo en verde.

#### Phase 4: Documentación
- [x] `docs/architecture/distribution-config-schema.md` — schema de `AccountBalanceSource`/
      `AccountBalanceCheckTarget` actualizado, tabla de mapeo `AccountType`→campo, la excepción WELLI,
      y el gap de `investmentGains` documentado explícitamente como no implementado.

### Next Step
Todas las fases completas. Falta correr `./scripts/run-tests.sh` (Docker) para confirmar contra el
suite completo según la convención del repo.
