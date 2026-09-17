**Created at**: 2026-09-16
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Calcular assignments por regla de Distribution Rules

### Executive Summary
El motor de ejecución ahora sabe calcular cuánto le corresponde a cada owner configurado en Distribution Rules, a partir del pool de fondos ya resuelto y particionado. Esto cierra el Bloque 4 (VPR-9668) del pipeline de distribución: antes de esta historia, la config de reglas se guardaba pero nunca se aplicaba en runtime.

### Technical Summary
- Nuevo `domain/model/Assignment.java` (`owner`, `amount`) — resultado de repartir el pool entre owners.
- Nuevo `CalculateAssignmentsUseCase`: implementa las 5 estrategias de `AmountDistributionStrategy` (`DEFAULT`, `PERCENTAGE_OF_POOL`, `FIXED_AMOUNT`, `PROPORTIONAL_WEIGHT`, `PERCENTAGE_OF_REMAINING`), con guardrail E6 (la suma asignada nunca supera el total del pool) y remanente sin reclamar cayendo al owner por default (`companyId`) o a un override de `remainingBalance`.
- Dos excepciones nuevas mapeadas a 409 en `GlobalExceptionHandler`: `AssignmentAllocationExceedsPoolException`, `AmbiguousDefaultDistributionStrategyException`.
- `RunDistributionUseCase`/`DistributionExecutionResult` extendidos con el nuevo paso/campo `assignments: List<Assignment>`.
- Explícitamente fuera de alcance (queda para un ticket futuro de "balance engine"): `sufficiencyStrategy`, `accountTransferRules`, `deductions[]`, `amountField` de `BalanceStrategyConfig`, y cualquier concepto de tiers LENDER/BORROWER/REST.
- Tests: `CalculateAssignmentsUseCaseTest` (11 casos, AAA/Mockito/AssertJ) + `RunDistributionUseCaseTest` actualizado. Corridos vía `./scripts/run-tests.sh`, confirmados pasando por el usuario.
- Fix post-implementación: el guardrail E6 chequeaba solo la suma final, no cada paso — una regla `FIXED_AMOUNT`/`PERCENTAGE_OF_POOL` que excede el pool podía quedar "absorbida" por una regla `DEFAULT` como un assignment negativo sin sentido, sin fallar. Se movió el chequeo a inmediatamente después de cada regla explícita, para fallar apenas se detecta sobre-asignación.

### Phases Completed
- [x] **Phase 1: CalculateAssignmentsUseCase con las 5 estrategias y guardrails** — nuevo use case, dominio y excepciones, con su suite de tests.
- [x] **Phase 2: Wiring en el orquestador de ejecución** — integrado en `RunDistributionUseCase`/`DistributionExecutionResult`.
- [x] **Phase 3: Chequeo de balance real** — investigando `master-trust-servicer-api` se confirmó que el balance check (a diferencia de `distributionStrategy`) sí es un mecanismo real usado en producción (WELLI/ADDI/Delta Credit contra `account_balance`), así que se trajo a esta historia: `AccountBalanceEntity`/`AccountBalanceJPARepository` (solo lectura), `BalanceSufficiencyStrategy` renombrado 1:1 al enum real, `accountIdsToCheck` agregado a `BalanceStrategyConfig`, y `CheckAccountBalanceSufficiencyUseCase` wireado en `CalculateAssignmentsUseCase`.
