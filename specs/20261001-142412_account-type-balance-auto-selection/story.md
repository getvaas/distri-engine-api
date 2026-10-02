**Created at**: 2026-10-01
**Status**: Done
**Original input**: @specs/20261001-142412_account-type-balance-auto-selection/original_request.md
**Plan implemented**: @specs/20261001-142412_account-type-balance-auto-selection/plan.md

# Story: Derivar el campo de balance a usar del tipo real de cuenta, no de una selección manual

### Description
Hoy, cuando una regla de Distribution Rules necesita chequear el balance real de una cuenta
(`sufficiencyStrategy`), este motor siempre suma `projectedBalance ?: currentBalance` para todas las
cuentas por igual — no existe ningún concepto de "tipo de cuenta" en la config. El sistema real
(`master-trust-servicer-api`) sí distingue: `COLLECTION`/`RESERVE` usan ese mismo fallback, pero
`INVESTMENT` además calcula una ganancia de inversión (`investmentGains`, fuera de alcance acá), y una
cuenta puntual (`WELLI_INVESTMENT`, identificada por código) fuerza `currentBalance` e ignora
`projectedBalance` sin importar su tipo. Esta historia agrega el tipo de cuenta a la config (en vez
de requerir que alguien elija manualmente qué campo de balance usar) y lo aplica al calcular el
balance disponible — documentando explícitamente el gap de `investmentGains` en vez de inventarlo.

### Acceptance Criteria
- [ ] **Given** una cuenta configurada en `accountIdsToCheck` con `accountType=COLLECTION` o
      `RESERVE`, **When** se calcula su balance disponible para un chequeo de suficiencia, **Then**
      se usa `projectedBalance ?: currentBalance` (mismo comportamiento que hoy).
- [ ] **Given** una cuenta con `accountType=INVESTMENT` y `accountCode` distinto de
      `WELLI_INVESTMENT`, **When** se calcula su balance disponible, **Then** se usa
      `projectedBalance ?: currentBalance` igual que las demás, y queda documentado (no implementado)
      que el sistema real además sumaría `investmentGains` ahí.
- [ ] **Given** una cuenta con `accountCode=WELLI_INVESTMENT` (sin importar su `accountType`),
      **When** se calcula su balance disponible, **Then** se usa siempre `currentBalance`, ignorando
      `projectedBalance`.
- [ ] **Given** la config persistida de una distribución (`BalanceStrategyConfig.accountIdsToCheck` y
      `AccountBalancePoolConfig.accounts`), **When** se guarda, **Then** cada cuenta lleva su
      `accountType` (y `accountCode` cuando aplica para la excepción) — ya no existe un campo de
      selección manual de "qué balance usar".

### Additional Context
**Fuera de alcance explícito**:
- El cálculo real de `investmentGains` (requiere `lastDistribution`, fórmula no verificada) — queda
  como gap documentado, no se inventa.
- La resolución del `AccountType`/`accountCode` reales de una cuenta — llegan ya resueltos como
  parámetro (el caller, hoy el wizard de `vaas-backoffice` vía Company API, se lo pasa a este motor).
- Cambios en `vaas-backoffice` (fuera de este repo).
- El Pool Strategy `ACCOUNT_BALANCE` no tiene todavía ningún resolver de ejecución
  (`ResolveEligibleFundsUseCase` falla explícito con `UnsupportedPoolStrategyException` si se
  selecciona) — el único consumidor real en ejecución hoy es el chequeo de suficiencia de balance
  (`CheckAccountBalanceSufficiencyUseCase`, vía `ComponentOwnerRule.balanceStrategy.sufficiencyStrategy`).
  Esta historia corrige la config de `AccountBalancePoolConfig.accounts` por consistencia (el usuario
  pidió explícitamente que "en config también tiene que estar"), pero no agrega un resolver de
  ejecución para ACCOUNT_BALANCE — eso sigue sin implementarse, sin relación con esta historia.

**Decisiones ya cerradas con el usuario**:
- `AccountType { COLLECTION, INVESTMENT, RESERVE }` — mismos 3 valores que el sistema real
  (`AccountType.kt`), verificado contra la rama `develop`.
- Mapeo: `COLLECTION`/`RESERVE` → `usableBalance` (`projected ?: current`); `INVESTMENT` → mismo
  fallback + gap documentado de `investmentGains`; excepción por `accountCode=WELLI_INVESTMENT` →
  siempre `currentBalance`, sin importar el tipo.
- El `AccountType`/`accountCode` se reciben como parámetro ya resuelto, no se resuelven acá.
