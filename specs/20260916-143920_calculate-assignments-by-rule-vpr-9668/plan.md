**Created at**: 2026-09-16
**Status**: In Progress
**Based on story**: @story.md

# Plan: Calcular assignments por regla de Distribution Rules

### Goal
Implementar el paso 7 del pipeline de ejecución (Bloque 4, VPR-9668): calcular, a partir del pool ya resuelto y particionado (VPR-9662 a 9667), cuánto le corresponde a cada owner configurado en Distribution Rules (VPR-9643), aplicando su `distributionStrategy`, garantizando que nunca se asigne más plata de la que hay, y resolviendo el remanente sin reclamar hacia el owner por default o un override explícito.

### Context
- `domain/model/DistributionRulesConfig.java`, `ComponentOwnerRule.java`, `BalanceStrategyConfig.java`, `RemainingBalanceConfig.java` — config ya construida (VPR-9643), nunca aplicada en runtime; esta historia la aplica.
- `domain/model/enums/AmountDistributionStrategy.java` — los 5 modos a implementar (`DEFAULT`, `PROPORTIONAL_WEIGHT`, `PERCENTAGE_OF_POOL`, `PERCENTAGE_OF_REMAINING`, `FIXED_AMOUNT`).
- `domain/model/PartitionedPoolFunds.java`, `PoolFund.java` — entrada del cálculo (`distributable`).
- `application/usecase/RunDistributionUseCase.java`, `domain/model/DistributionExecutionResult.java` — orquestador y resultado a extender con el nuevo paso.
- `application/usecase/ResolveActiveDistributionConfigUseCase.java` — mismo patrón de resolución de config activa que ya usa `ResolveEligibleFundsUseCase`.
- `infrastructure/web/GlobalExceptionHandler.java` — registrar la nueva excepción (409), mismo patrón que el resto del repo.

### Public Contracts
- **Domain**: `Assignment(String owner, BigDecimal amount)` — record nuevo en `domain/model/`.
- **Services**:
  - `CalculateAssignmentsUseCase.execute(Long companyId, PartitionedPoolFunds funds): List<Assignment>` — resuelve la config activa internamente, lee `config.rules()`.
- **Domain Exceptions**:
  - `AssignmentAllocationExceedsPoolException` — la suma de lo asignado por todas las reglas supera el total del pool (E6). Mapea a 409.
  - `AmbiguousDefaultDistributionStrategyException` — más de una `ComponentOwnerRule` con estrategia `DEFAULT` (o `balanceStrategy=null`) en la misma config. Mapea a 409.
- **Reglas de cálculo** (sobre `totalPool = sum(funds.distributable().amount())`):
  - `PERCENTAGE_OF_POOL` → `totalPool * distributionValue/100`.
  - `FIXED_AMOUNT` → `distributionValue`.
  - `PROPORTIONAL_WEIGHT` → `totalPool * (peso de la regla / suma de pesos de todas las reglas PROPORTIONAL_WEIGHT)`.
  - `PERCENTAGE_OF_REMAINING` → en cascada, en orden de configuración: `remanente_actual * distributionValue/100`.
  - `DEFAULT` (o `balanceStrategy=null`) → se evalúa al final, recibe todo lo que quede sin reclamar. Máximo una por config.
  - Remanente final sin reclamar (incluye el caso `hasComponentOwners=false`/`componentOwners` vacío): `Assignment(String.valueOf(companyId), remanente)`, salvo `remainingBalance` configurado → `Assignment(String.valueOf(remainingBalance.destinationAccountId()), remanente)`.
- **Tests**: `CalculateAssignmentsUseCaseTest` (una regla DEFAULT, mezcla de estrategias, over-allocation, DEFAULT ambiguo, sin reglas, remainingBalance override), `RunDistributionUseCaseTest` actualizado.

### Phases

#### Phase 1: CalculateAssignmentsUseCase con las 5 estrategias y guardrails
- [x] Crear `domain/model/Assignment.java`.
- [x] Crear `application/usecase/AssignmentAllocationExceedsPoolException.java` y `AmbiguousDefaultDistributionStrategyException.java`.
- [x] Crear `application/usecase/CalculateAssignmentsUseCase.java` con las 5 estrategias, el guardrail E6, y el remanente a owner default/override.
- [x] Registrar ambas excepciones nuevas en `GlobalExceptionHandler` (409).
- [x] Tests unitarios AAA (Mockito/AssertJ) cubriendo cada estrategia y los casos límite del story.

#### Phase 2: Wiring en el orquestador de ejecución
- [x] Agregar `assignments: List<Assignment>` a `DistributionExecutionResult`.
- [x] Llamar `CalculateAssignmentsUseCase` desde `RunDistributionUseCase` después de la partición por ownership.
- [x] Actualizar `RunDistributionUseCaseTest` para el nuevo campo/paso.

#### Phase 3: Chequeo de balance real (`sufficiencyStrategy`)
Investigando el motor real (`master-trust-servicer-api`) surgió que el chequeo de balance sí es un
mecanismo real y usado en producción (WELLI/ADDI/Delta Credit), a diferencia de `distributionStrategy`
(que no tiene precedente real) — se decide traerlo a esta historia en vez de dejarlo para un ticket
futuro.
- [x] Agregar `accountIdsToCheck: List<Long>` a `BalanceStrategyConfig`/`BalanceStrategyConfigRequest` (no existía; el real `BalanceRule.accountIdsToCheck` lo requiere).
- [x] Renombrar `BalanceSufficiencyStrategy` para matchear 1:1 el enum real `BalanceStrategy`: `SUFFICIENT_BALANCE_OR_STOP`, `SUFFICIENT_BALANCE_OR_SKIP_ALL_BORROWERS`, `UNTIL_BALANCE_EXHAUSTED_WHILE_FITTING_PAYMENTS` (se elimina `IGNORE_BALANCE`; sin chequeo = `sufficiencyStrategy=null`).
- [x] Nueva entity de solo lectura `AccountBalanceEntity`/`AccountBalanceJPARepository` sobre `account_balance` (verificado contra `V1.0.11__create_account_balance.sql`).
- [x] Nuevo `CheckAccountBalanceSufficiencyUseCase`: resuelve balance disponible (`projectedBalance ?? currentBalance`, sumado por cuenta) y aplica la estrategia — `SKIP_ALL_BORROWERS` simplificado a "saltar solo esa regla" (sin tiers), `UNTIL_EXHAUSTED` simplificado a capar el monto al balance disponible (sin fitting por payment individual, decisiones confirmadas con el usuario).
- [x] Nueva excepción `InsufficientAccountBalanceException` (409) para `SUFFICIENT_BALANCE_OR_STOP`.
- [x] Wiring en `CalculateAssignmentsUseCase`: el chequeo se aplica al monto reclamado por cualquier regla (incluida `DEFAULT`) antes de restarlo del pool.
- [x] Tests nuevos (balance suficiente, insuficiente con STOP/SKIP/UNTIL_EXHAUSTED, sin `accountIdsToCheck` configurado, sin `sufficiencyStrategy`) + `DistributionRulesConfigBuilderTest` actualizado para el nuevo campo/enum.

### Next Step
Las 3 fases están implementadas. Pendiente: correr `./scripts/run-tests.sh` y confirmar que pasan antes de volver a marcar Status Done.
