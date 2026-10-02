**Created at**: 2026-10-01
**Based on plan**: @specs/20261001-142412_account-type-balance-auto-selection/plan.md
**Based on story**: @specs/20261001-142412_account-type-balance-auto-selection/story.md

# Resume: Derivar el campo de balance a usar del tipo real de cuenta

### Executive Summary
Antes había que elegir manualmente qué campo de balance leer para cada cuenta (saldo actual vs.
saldo disponible). Ahora el motor lo deriva automáticamente a partir del tipo real de la cuenta
(cobranza, inversión, reserva), igual que lo hace el sistema real — incluyendo la excepción puntual
de una cuenta específica (WELLI) que siempre usa su saldo actual. El cálculo de ganancias de
inversión que el sistema real suma para cuentas de inversión queda documentado como pendiente, no
inventado.

### Technical Summary
- `AccountType` enum (`COLLECTION`/`INVESTMENT`/`RESERVE`) nuevo, verificado contra
  `master-trust-servicer-api` (rama `develop`).
- `ResolveAccountBalanceFieldUseCase`: deriva `PoolBalanceType` a partir de `AccountType` +
  `accountCode` — excepción por código `WELLI_INVESTMENT` fuerza `CURRENT_BALANCE`; cualquier otro
  caso usa `USABLE_BALANCE`; logea (no calcula) el gap de `investmentGains` para `INVESTMENT`.
- `BalanceStrategyConfig.accountIdsToCheck: List<Long>` → `accountChecks: List<AccountBalanceCheckTarget>`
  (cambio de forma, sin shim de compatibilidad — no había configs reales en producción).
  `CheckAccountBalanceSufficiencyUseCase` ahora resuelve el campo de balance por cuenta en vez de
  aplicar la misma fórmula fija a todas.
- `AccountBalanceSource.balanceType: PoolBalanceType` → `accountType`/`accountCode` (Pool Strategy
  `ACCOUNT_BALANCE`, config-only — sin resolver de ejecución todavía para esa estrategia).
- `docs/architecture/distribution-config-schema.md` actualizado con el schema nuevo y la tabla de
  mapeo tipo→campo.
- Sin cambios en `vaas-backoffice` (fuera de alcance) ni en el cálculo real de `investmentGains`
  (gap documentado explícitamente, requiere `lastDistribution`, no verificado).

### Phases Completed
- [x] **Phase 1**: AccountType + resolver — enum nuevo y `ResolveAccountBalanceFieldUseCase` con 6 tests.
- [x] **Phase 2**: Balance Sufficiency Check — `accountChecks` reemplaza `accountIdsToCheck`, el
      consumidor de ejecución real ahora resuelve el campo por cuenta, 6 tests nuevos + ajustes en
      `CalculateAssignmentsUseCaseTest`.
- [x] **Phase 3**: Pool Strategy `ACCOUNT_BALANCE` — config actualizada por consistencia (sin
      resolver de ejecución todavía), `PoolConfigBuilderTest` ajustado.
- [x] **Phase 4**: Documentación — schema y tabla de mapeo en `distribution-config-schema.md`.

`./gradlew test` completo en verde (43 clases). Falta `./scripts/run-tests.sh` (Docker) para
confirmar contra el suite completo según la convención del repo.
