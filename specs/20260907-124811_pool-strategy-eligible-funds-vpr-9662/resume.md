**Created at**: 2026-09-07
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Resolver el pool de fondos elegibles según Pool Strategy, y conectar el proceso base

### Executive Summary
El motor de ejecución ahora puede resolver, de verdad, el pool de fondos de una distribución según su Pool Strategy configurada — y por primera vez hay un proceso base real que conecta las precondiciones (readiness checks) con esa resolución, en vez de piezas sueltas probadas solo por separado.

### Technical Summary
- `FetchEligiblePaymentTapesUseCase` (ex `FetchCandidatePaymentTapesUseCase`) ahora resuelve también `amount` según `amountField` (`net_amount`/`gross_amount` mapeados en `PaymentTapeEntity`); otro valor real pero no mapeado falla explícito (`UnsupportedPoolAmountFieldException`) en vez de devolver un monto incorrecto.
- `PoolStrategyResolver` (interfaz, mismo patrón que `ReadinessCheck`) + `PaymentTapePoolStrategyResolver` (única implementación real) + `ResolveEligibleFundsUseCase` (dispatcher): evalúa `pool.strategy()` de la config activa y delega; `ACCOUNT_BALANCE`/`DATA_SOURCE_AGGREGATION` fallan explícito (`UnsupportedPoolStrategyException`) — no hay resolver real todavía para esas dos.
- Se eliminó el endpoint de diagnóstico (`GET /distributions/candidates`) y se renombró `Candidate` → `Eligible` en todo el módulo — mismo criterio que los readiness checks: es una pieza interna, no una operación pública.
- **`RunDistributionUseCase`** (nuevo): el primer orquestador real del motor — resuelve la config activa una sola vez, corre `RunReadinessChecksUseCase`, y solo si `readyToDistribute` corre `ResolveEligibleFundsUseCase`. Devuelve `DistributionExecutionResult(readiness, funds)`.
- Explícitamente fuera de alcance (documentado, no construido): frequency checker, ownership, assignments, persistencia real, notificaciones, y el fallback de 3 niveles de `netAmount` de VPR-9628.
- Decisión de diseño: no se unificaron las firmas de `RunReadinessChecksUseCase` (toma `id`) y `ResolveEligibleFundsUseCase` (toma `companyId`) — el orquestador resuelve `companyId → config` una vez y pasa el `id` resultante; cada use case interno resuelve por su cuenta (lookup barato por PK), sin necesidad de optimizar esa doble resolución.

### Phases Completed
- [x] **Phase 1**: Renombrar `Candidate`→`Eligible`, eliminar el endpoint de diagnóstico.
- [x] **Phase 2**: Resolver `amount` por Pool Strategy — `PoolFund`, `PoolStrategyResolver`, `PaymentTapePoolStrategyResolver`, `ResolveEligibleFundsUseCase`.
- [x] **Phase 3**: `RunDistributionUseCase` — el orquestador base, conecta readiness checks + resolución de pool.
