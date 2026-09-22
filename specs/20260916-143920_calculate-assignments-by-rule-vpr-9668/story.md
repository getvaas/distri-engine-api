**Created at**: 2026-09-16
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: @specs/20260916-143920_calculate-assignments-by-rule-vpr-9668/plan.md

# Story: Calcular assignments por regla de Distribution Rules

### Description
Hoy el motor de ejecución resuelve el pool de fondos elegibles de una distribución y los particiona entre distribuibles y sin owner (VPR-9662 a 9667), pero no sabe todavía cuánto le corresponde a cada owner configurado en Distribution Rules (VPR-9643) — esa config solo se guarda, nunca se aplica. Esta historia cierra ese paso: por cada `ComponentOwnerRule` activa, calcula el monto que le toca según su estrategia de distribución, deja el remanente en manos del owner por default (o de un override explícito si el deal lo configuró), y garantiza que nunca se asigne más plata de la que realmente hay en el pool.

### Acceptance Criteria
- [ ] **Given** una distribución lista para distribuir con una única `ComponentOwnerRule` (estrategia `DEFAULT`), **When** se calculan los assignments, **Then** esa regla recibe el 100% del pool distribuible bajo su `owner`.
- [ ] **Given** varias `ComponentOwnerRule` activas con `distributionStrategy` `PERCENTAGE_OF_POOL`/`PERCENTAGE_OF_REMAINING`/`FIXED_AMOUNT`/`PROPORTIONAL_WEIGHT`, **When** se calculan los assignments, **Then** cada una recibe el monto correspondiente a su estrategia, evaluadas en orden de configuración.
- [ ] **Given** la suma de lo asignado por todas las reglas activas supera el total del pool, **When** se calculan los assignments, **Then** la corrida falla explícito (no se genera un resultado incorrecto).
- [ ] **Given** queda un remanente sin reclamar por ninguna regla y `remainingBalance` no está configurado, **When** se calculan los assignments, **Then** ese remanente se asigna al owner ya resuelto del fondo (el borrower/company de la distribución).
- [ ] **Given** queda un remanente sin reclamar por ninguna regla y `remainingBalance` sí está configurado, **When** se calculan los assignments, **Then** ese remanente se asigna según el override de `remainingBalance` en vez del default.
- [ ] **Given** `hasComponentOwners=false` o no hay ninguna `ComponentOwnerRule` configurada, **When** se calculan los assignments, **Then** todo el pool queda asignado al owner ya resuelto del fondo (comportamiento equivalente al caso "sin reglas que reclamen nada").

### Additional Context
Fuera de alcance explícito de esta historia (queda para un ticket futuro de "balance engine"): `sufficiencyStrategy`, `accountTransferRules`, `deductions[]` y `amountField` de `BalanceStrategyConfig` — es decir, todavía no se mueve plata entre cuentas ni se aplican descuentos/fees. Tampoco se modela ningún concepto de tiers LENDER/BORROWER/REST del sistema legacy — cada assignment es simplemente owner + amount. No incluye persistencia de la distribución ni marcado de payment tapes (VPR-9669), ni ningún endpoint/scheduler nuevo para disparar la corrida.
