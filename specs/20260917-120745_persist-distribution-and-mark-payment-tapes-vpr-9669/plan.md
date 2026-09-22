**Created at**: 2026-09-17
**Status**: Done
**Based on story**: @story.md

# Plan: Persistir la distribución y marcar payment tapes como distribuidos

### Goal
Persistir la distribución calculada (`Distribution`+`Assignment`) directo en las tablas reales de `master_trust_servicer` (mismo patrón interno que `CreateDistribution.apply()`), y marcar cada payment tape distribuido con su `distributionId` en `payments_db` — cerrando el Bloque 5 del pipeline de ejecución.

### Context
- `domain/model/PoolFund.java` — pierde la fecha del payment tape al colapsar el pool; hace falta recuperarla.
- `application/usecase/PaymentTapePoolStrategyResolver.java` — mapea `EligiblePaymentTape`→`PoolFund`, punto donde se propaga `paymentDate`.
- `infrastructure/persistence/masterservicer/entity/MasterServicerDistributionEntity.java` — hoy solo lectura (VPR-9661), le faltan columnas para poder escribir una distribución completa.
- `infrastructure/config/MasterServicerDataSourceConfig.java` / `PaymentsDataSourceConfig.java` — transaction managers ya definidos (`masterServicerTransactionManager`, `paymentsTransactionManager`), dos datasources sin transacción distribuida entre sí.
- `infrastructure/persistence/payments/PaymentTapeJPARepository.java`, `entity/PaymentTapeEntity.java` — ya tiene `distributionId: String`, falta el método de escritura.
- `domain/model/DistributionExecutionResult.java`, `application/usecase/RunDistributionUseCase.java` — punto de wiring final.
- `domain/model/DistributionConfig.java` (`masterTrustId`, `config.currency`), `domain/model/ComponentOwnerRule.java` (`description`, `toAccountId`), `domain/model/Assignment.java` (`owner`, `accountId`, `amount`).

### Public Contracts
- **Domain**: `PoolFund(String sourceId, BigDecimal amount, String owner, LocalDateTime paymentDate)`.
- **Domain**: `Assignment` gana `concept: String` — `Assignment(String owner, Long accountId, String concept, BigDecimal amount)`. Se resuelve en `CalculateAssignmentsUseCase` (donde todavía se tiene la `ComponentOwnerRule`, VPR-9668): `rule.description()` si no es null/blank, si no `rule.owner()`; para el remanente, el mismo owner ya resuelto (`defaultOwner(...)`). Necesario porque `Assignment` no referencia la regla que lo originó — sin este campo, `PersistDistributionUseCase` no tendría de dónde sacar `assignment.concept`.
- **Persistence (escritura, `master_trust_servicer`)**:
  - `MasterServicerDistributionEntity` extendida: `+ firstPaymentDate, lastPaymentDate, creationDate, lastUpdateDate` (columnas ya existentes en la tabla real, sin mapear hasta ahora).
  - `AssignmentEntity` (nueva): `id, accountId, amount, currency, active, creationDate, lastUpdateDate, concept, fromAccountId` (nullable) — cascadeada desde `MasterServicerDistributionEntity` vía `@OneToMany`/`@JoinTable` hacia `distribution_assignments`; no hizo falta un repository propio.
- **Persistence (escritura, `payments_db`)**: `MarkPaymentTapesAsDistributedUseCase` usa `findById`/`save` ya disponibles en `PaymentTapeJPARepository` (respetando el `@IdClass` por `companyId`) — no hizo falta un método nuevo en el repositorio.
- **Services**:
  - `PersistDistributionUseCase.execute(DistributionConfig config, LocalDate date, PartitionedPoolFunds funds, List<Assignment> assignments): MasterServicerDistributionEntity` — `status=CALCULATED` si `assignments` no está vacío, si no `NOTHING_DISTRIBUTABLE`; `currency=config.config().currency()`; `concept=assignment.concept()` (ya resuelto, ver contrato de `Assignment` arriba); `firstPaymentDate`/`lastPaymentDate` = min/max de `PoolFund.paymentDate()` sobre `funds.distributable()`. Transaccional (`masterServicerTransactionManager`).
  - `MarkPaymentTapesAsDistributedUseCase.execute(Long companyId, String distributionId, List<PoolFund> distributable): void` — solo toca `distributable`, nunca `ownerless`. Transaccional (`paymentsTransactionManager`).
- **Tests**: `PersistDistributionUseCaseTest` (CALCULATED/NOTHING_DISTRIBUTABLE, currency/concept, first/last payment date), `MarkPaymentTapesAsDistributedUseCaseTest`, `RunDistributionUseCaseTest` actualizado, tests existentes de `PoolFund`/`PaymentTapePoolStrategyResolverTest` actualizados para el nuevo campo.

### Phases

#### Phase 1: `PoolFund.paymentDate` y `Assignment.concept`
- [x] Agregar `paymentDate: LocalDateTime` a `PoolFund`, propagado desde `PaymentTapePoolStrategyResolver` (`EligiblePaymentTape.paymentDate()`).
- [x] Agregar `concept: String` a `Assignment`, resuelto en `CalculateAssignmentsUseCase` (`rule.description() ?: rule.owner()`, o el owner del remanente).
- [x] Actualizar todos los tests existentes que construyen `PoolFund`/`Assignment` (`ResolveEligibleFundsUseCaseTest`, `CalculateAssignmentsUseCaseTest`, `RunDistributionUseCaseTest`, `PartitionOwnershipUseCaseTest`, `PaymentTapePoolStrategyResolverTest`).

#### Phase 2: Persistencia de Distribution + Assignment
- [x] Extender `MasterServicerDistributionEntity` con las columnas faltantes (`firstPaymentDate`, `lastPaymentDate`, `creationDate`, `lastUpdateDate`) + relación `@OneToMany`/`@JoinTable` hacia `distribution_assignments`.
- [x] Crear `AssignmentEntity` (escritura, cascadeada desde `MasterServicerDistributionEntity` — no hizo falta un repository propio).
- [x] Crear `PersistDistributionUseCase` (status, currency, concept, first/last payment date), transaccional (`masterServicerTransactionManager`).
- [x] Tests: `PersistDistributionUseCaseTest` (Mockito, lógica de negocio) + `MasterServicerDistributionJPARepositoryTest` ampliado con un caso `@DataJpaTest` verificando el cascade real hacia `distribution_assignments`.

#### Phase 3: Marcado de payment tapes + wiring final
- [x] Crear `MarkPaymentTapesAsDistributedUseCase` (usa `findById`/`save` ya disponibles en `PaymentTapeJPARepository`, sin método nuevo), transaccional (`paymentsTransactionManager`), solo sobre `distributable`. Nueva `PaymentTapeNotFoundException` (404) si un fund no tiene payment tape real.
- [x] `DistributionExecutionResult` gana `distributionId: Long`.
- [x] Wireados ambos use cases al final de `RunDistributionUseCase` (persistir primero, marcar después).
- [x] Tests: `MarkPaymentTapesAsDistributedUseCaseTest` (nuevo) + `RunDistributionUseCaseTest` actualizado.

### Next Step
All phases completed. See resume.md.
