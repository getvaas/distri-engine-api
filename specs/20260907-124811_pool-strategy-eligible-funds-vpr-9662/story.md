**Created at**: 2026-09-07
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Resolver el pool de fondos elegibles según Pool Strategy, y conectar el proceso base

### Description
El motor de ejecución ya sabía calcular la ventana de payment tapes elegibles, pero solo como un
cálculo aislado y expuesto por un endpoint de diagnóstico — nada lo usaba para procesar de verdad.
Además, no había ningún punto que evaluara explícitamente qué Pool Strategy tiene configurada una
company (`PAYMENT_TAPE`, `ACCOUNT_BALANCE`, `DATA_SOURCE_AGGREGATION`) antes de intentar resolver
el pool, ni un orquestador que conectara esto con las precondiciones (readiness checks, VPR-9661)
ya construidas. Sin esto, cada pieza del motor quedaba aislada y no probada en conjunto.

### Acceptance Criteria
- [x] **Given** una config con `pool.strategy = PAYMENT_TAPE`, **When** se resuelve el pool de fondos, **Then** devuelve un `PoolFund` por cada payment tape elegible, con su `amount` resuelto según `amountField` (`net_amount`/`gross_amount`).
- [x] **Given** una config con `pool.strategy = ACCOUNT_BALANCE` o `DATA_SOURCE_AGGREGATION`, **When** se resuelve el pool, **Then** falla explícito (`UnsupportedPoolStrategyException`) en vez de devolver datos vacíos o incorrectos — esas estrategias no tienen resolver real todavía.
- [x] **Given** un `amountField` que apunta a una columna real de `payment_tape` pero no mapeada todavía, **When** se resuelve el monto, **Then** falla explícito (`UnsupportedPoolAmountFieldException`).
- [x] **Given** una company con config activa, **When** se corre el proceso base de distribución, **Then** se resuelve la config una sola vez, se corren las precondiciones, y solo si están listas se resuelve el pool de fondos — si no están listas, el pool nunca se calcula.
- [x] El endpoint de diagnóstico (`GET /distributions/candidates`) ya no existe — la resolución de fondos es una pieza interna del motor, no una operación pública.

### Additional Context
- Jira: VPR-9662 — continuación de `specs/20260821-155208_payment-tape-search-window-working-days-vpr-9662` (Done: ventana de fechas + query).
- Referencia real usada para validar el diseño: trace de 11 pasos de una distribución real (payjoy/ADELANTOS) en `master-trust-servicer-api`, compartido por el usuario.
- Fuera de alcance (confirmado explícitamente): frequency checker (DAILY/WEEKLY/MONTHLY), ownership, assignments, persistencia real de `distribution`/`assignment`, notificaciones, y el fallback de 3 niveles de `netAmount` (VPR-9628) — ninguno de estos existe todavía en el motor.
- `RunReadinessChecksUseCase` sigue tomando `id` (no `companyId`) — decisión explícita de no tocar esa firma; el orquestador resuelve `companyId → config` una sola vez y le pasa el `id` resultante, sin necesidad de unificar las firmas de los use cases internos.
