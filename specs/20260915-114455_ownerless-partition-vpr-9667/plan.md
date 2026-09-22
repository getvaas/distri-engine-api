**Created at**: 2026-09-15
**Status**: Done
**Based on story**: @story.md

# Plan: Particionar el pool entre distribuibles y ownerless

### Goal
Construir `PartitionOwnershipUseCase` y `PartitionedPoolFunds`, e integrarlos en `RunDistributionUseCase`
para que el orquestador devuelva el pool ya particionado.

### Context
- `src/main/java/com/getvaas/distribution/engine/domain/model/PartitionedPoolFunds.java` — nuevo.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/PartitionOwnershipUseCase.java` — nuevo.
- `src/main/java/com/getvaas/distribution/engine/domain/model/DistributionExecutionResult.java` — `funds` pasa de `List<PoolFund>` a `PartitionedPoolFunds`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/RunDistributionUseCase.java` — invoca la partición después de resolver el pool.

### Public Contracts
- **Domain model**: `PartitionedPoolFunds(distributable: List<PoolFund>, ownerless: List<PoolFund>)`
- **Services**: `PartitionOwnershipUseCase.execute(List<PoolFund>) -> PartitionedPoolFunds`

### Phases

#### Phase 1: Partición y wiring
- [x] `PartitionedPoolFunds(distributable, ownerless)`.
- [x] `PartitionOwnershipUseCase`: un fondo es ownerless si `owner == ResolveOwnershipUseCase.UNDEFINED_OWNER`.
- [x] `DistributionExecutionResult.funds` pasa a `PartitionedPoolFunds`; `RunDistributionUseCase` particiona después de resolver el pool.
- [x] `PartitionOwnershipUseCaseTest` (4 tests) + `RunDistributionUseCaseTest` actualizado (partición end-to-end).

### Next Step
Fase implementada y verificada con una corrida real de tests en verde (`./scripts/run-tests.sh`,
confirmado por el usuario). Ver `resume.md`.
