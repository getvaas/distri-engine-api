**Created at**: 2026-09-07
**Status**: Done
**Based on story**: @story.md

# Plan: Resolver el pool de fondos elegibles según Pool Strategy, y conectar el proceso base

### Goal
Reemplazar el endpoint diagnóstico de candidatos por un dispatcher que evalúa Pool Strategy y
resuelve el pool real (con monto), y construir el orquestador base que conecta readiness checks +
resolución de pool en un único proceso.

### Context
- `src/main/java/com/getvaas/distribution/engine/application/usecase/FetchEligiblePaymentTapesUseCase.java` — renombrado de `FetchCandidatePaymentTapesUseCase`, ahora también resuelve `amount`.
- `src/main/java/com/getvaas/distribution/engine/domain/service/pool/PoolStrategyResolver.java` — interfaz nueva, mismo patrón que `ReadinessCheck`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/PaymentTapePoolStrategyResolver.java`, `ResolveEligibleFundsUseCase.java`, `RunDistributionUseCase.java` — nuevos.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/payments/entity/PaymentTapeEntity.java` — gana `netAmount`/`grossAmount`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/DistributionRouter.java`, `dto/PaymentTapeCandidateResponse.java` — eliminados.

### Public Contracts
- **Services**:
  - `PoolStrategyResolver.type() -> PoolStrategyType` / `resolve(companyId, date) -> List<PoolFund>`
  - `ResolveEligibleFundsUseCase.execute(companyId, date) -> List<PoolFund>`
  - `RunDistributionUseCase.execute(companyId, date) -> DistributionExecutionResult`
  - `FetchEligiblePaymentTapesUseCase.execute(companyId, date) -> List<EligiblePaymentTape>` (ahora con `amount`)
- **Domain model**: `PoolFund(sourceId, amount)`, `EligiblePaymentTape(id, companyId, paymentDate, amount)`, `DistributionExecutionResult(readiness, funds)`
- **Excepciones**: `UnsupportedPoolStrategyException` (409), `UnsupportedPoolAmountFieldException` (409)
- **Database**: sin migraciones — 2 columnas nuevas mapeadas sobre `payment_tape` ya existente (`net_amount`, `gross_amount`).

### Phases

#### Phase 1: Renombrar y limpiar la pieza de diagnóstico
- [x] `FetchCandidatePaymentTapesUseCase` → `FetchEligiblePaymentTapesUseCase`, `PaymentTapeCandidate` → `EligiblePaymentTape`.
- [x] Eliminar `DistributionRouter` (`GET /distributions/candidates`) y `PaymentTapeCandidateResponse` — pieza interna, no pública.

#### Phase 2: Resolver el monto por Pool Strategy
- [x] `PaymentTapeEntity` gana `netAmount`/`grossAmount`.
- [x] `FetchEligiblePaymentTapesUseCase` resuelve `amount` según `amountField` (`net_amount`/`gross_amount`); otro valor → `UnsupportedPoolAmountFieldException`.
- [x] `PoolFund(sourceId, amount)`, `PoolStrategyResolver` (interfaz), `PaymentTapePoolStrategyResolver` (única implementación real).
- [x] `ResolveEligibleFundsUseCase`: resuelve la config activa, evalúa `pool.strategy()`, delega al resolver registrado; sin resolver para la estrategia (o sin pool) → `UnsupportedPoolStrategyException`.

#### Phase 3: Orquestador base
- [x] `DistributionExecutionResult(readiness, funds)`.
- [x] `RunDistributionUseCase.execute(companyId, date)`: resuelve la config una vez, corre `RunReadinessChecksUseCase`, y si `readyToDistribute`, corre `ResolveEligibleFundsUseCase`. Si no está listo, `funds` queda vacía sin calcular el pool.

### Next Step
Las 3 fases están implementadas y verificadas con una corrida real de tests en verde
(`./scripts/run-tests.sh`, confirmado por el usuario). Ver `resume.md`.
