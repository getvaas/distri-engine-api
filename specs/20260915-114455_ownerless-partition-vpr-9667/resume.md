**Created at**: 2026-09-15
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Particionar el pool entre distribuibles y ownerless

### Executive Summary
El orquestador de distribución ahora separa el pool resuelto entre fondos con owner real y fondos sin owner ("ownerless"), un paso necesario antes de armar assignments — sin el riesgo del bug de NPE del sistema actual, porque el owner nunca es `null` en este motor.

### Technical Summary
- `PartitionOwnershipUseCase` (nuevo): clasifica cada `PoolFund` según su `owner` — `"UNDEFINED"` va a `ownerless`, cualquier otro valor va a `distributable`.
- `PartitionedPoolFunds(distributable, ownerless)` reemplaza el `List<PoolFund>` plano en `DistributionExecutionResult`.
- `RunDistributionUseCase` particiona automáticamente después de resolver el pool vía `ResolveEligibleFundsUseCase`.
- El bug real (`p.ownerName!!` → NPE) ya estaba resuelto de raíz desde VPR-9665; esta historia solo construyó la clasificación en sí.
- Fuera de alcance: notificaciones diferenciadas por motivo de ownerless, y el marcador `"UNKNOWN"` (exclusivo de Ownership API, diferido).

### Phases Completed
- [x] **Phase 1**: `PartitionedPoolFunds` + `PartitionOwnershipUseCase`, integrado en `RunDistributionUseCase` — implementado y testeado (7 tests).
