Jira ticket: VPR-9668 (https://pmvaas1.atlassian.net/browse/VPR-9668) — "Ejecución: armado de assignments por regla (pool, deducciones, balance check, tiers)".

## Descripción original del ticket

Bloque 4 — Se arman las asignaciones. El paso central: decidir a quién va la plata y cuánto, por cada regla configurada.

Hoy (verificado): `AssignmentsResolver.calculateAssignments()` — por cada `AssignmentConfig`: matchea criteria, suma `netAmount` de los tapes que matchean (pool de la regla), resta `reserveAmount` si está configurado (fee), corre el balance check si aplica (3 estrategias vivas + 1 comentada, `AssignmentsAccountsBalanceManager`), construye tiers LENDER/BORROWER/REST — el tier "resto" no recibe balance manager ni chequeo (agujero de sobre-giro documentado en E3).

Alimentado por: Distribution Rules (VPR-9643) — que hoy es la ÚNICA historia de esta etapa completa, y le faltan fees/deducciones, multi-moneda y regla de remanente (ver gaps ya anotados en VPR-9643).

Esta historia es donde se implementan, en runtime, las 3 extensiones que dejamos pendientes en VPR-9643:
1. `deductions[]` (E4) — hoy solo existe `reserveAmount` fijo, sin beneficiario.
2. Multi-moneda por regla (E9) — hoy `currency` se estampa del primer tape que matchea, bug de plata latente documentado.
3. Regla de remanente (Somos/Solvento/Finkargo) — hoy no existe ningún mecanismo genérico, cada uno lo resuelve en Python a mano.
4. El check obligatorio de "suma de componentes ≤ netAmount del tape" (E6) — hoy no existe; un tape que matchea 2 reglas se cuenta 2 veces.

Ref: docs/proceso-distribucion-unificado.md, Sección 1 paso 7 · epica-distri-engine.md E3, E4, E6, E9.

## Contexto conversacional y decisiones tomadas antes de escribir la historia

- El usuario y yo evaluamos si distri-engine-api puede correr una distribución real hoy — concluimos que no. Orden de lo que falta: VPR-9668 (assignments) → VPR-9669 (persistencia) → VPR-9670 (reporte); notificaciones (9671/72/73) de menor prioridad.
- Investigué el repo real (`docs/proceso-distribucion-unificado.md`, paso 7 del pipeline Kotlin) y confirmé que "Construir assignments" (paso 7) se alimenta de Distribution Rules (VPR-9643), ya construido en este repo como `DistributionRulesConfig`/`ComponentOwnerRule`/`BalanceStrategyConfig`/`AccountTransferRule`/`Deduction`/`RemainingBalanceConfig` — pero nunca aplicado en runtime.
- `PaymentTapeEntity` no tiene columnas de componente (`current_principal`/`current_interest`/`moratory_interest`/`current_guarantee`) — solo `netAmount`/`grossAmount`. El usuario aclaró que el "pool" ya viene resuelto por la Pool Strategy configurada (PAYMENT_TAPE con `amountField`/customField, ACCOUNT_BALANCE con `total_amount`, DATA_SOURCE_AGGREGATION con filtro+acumulado) — no hace falta re-derivar un pool por regla, `component` es metadata de a quién pertenece la regla, no un filtro de datos.
- Consulté al frontend (`vaas-backoffice`, wizard V2 de Distribution Rules, todavía mockeado/sin conectar) para validar si un split entre varias reglas vía `distributionStrategy`/`distributionValue` tiene sentido de negocio real. Confirmado: el wizard ya modela un concepto equivalente ("Amount strategy" con 5 modos: `DEFAULT_SUM`, `PROPORTIONAL_WEIGHT`, `PCT_OF_POOL`, `PCT_OF_REMAINING`, `FIXED_AMOUNT`), aunque con nombres de campo distintos a los nuestros y sin reconciliar todavía (esa reconciliación queda para cuando se conecte Rules en el V2, fuera de alcance de esta historia backend).
- Decisiones finales del usuario:
  1. Implementar ya el split entre reglas vía `distributionStrategy`/`distributionValue` (los 5 modos existentes en `BalanceStrategyConfig`), no limitarse a "una sola regla activa por corrida".
  2. Diferir explícitamente a un ticket futuro: `sufficiencyStrategy`, `accountTransferRules`, `deductions[]`, `amountField` de `BalanceStrategyConfig` (todo el "balance engine" de mover plata entre cuentas).
  3. El remanente sin reclamar por ninguna regla va por default al owner ya resuelto del fondo (el borrower/company de la distribución) — no falla. `remainingBalance`, si está configurado, es un override explícito sobre ese default.
  4. El check E6 (suma asignada ≤ total del pool) aplica como guardrail — falla explícito ante sobre-asignación.
  5. Sin concepto de tiers LENDER/BORROWER/REST (no existe en el modelo de distri-engine-api) — cada assignment es simplemente owner + amount.
