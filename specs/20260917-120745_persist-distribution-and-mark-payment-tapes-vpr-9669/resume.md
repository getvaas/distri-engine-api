**Created at**: 2026-09-17
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Persistir la distribución y marcar payment tapes como distribuidos

### Executive Summary
El motor de ejecución ahora deja registrada de verdad la distribución que calcula: graba `Distribution`+`Assignment` en las tablas reales de master-trust-servicer y marca cada payment tape usado como distribuido, para que no se vuelva a considerar en una corrida futura. Esto cierra el Bloque 5 (VPR-9669) — antes de esta historia, todo el cálculo del motor era pura simulación en memoria, sin ningún efecto persistente.

### Technical Summary
- Se escribe **directo** a las tablas `distribution`/`assignment`/`distribution_assignments` de `master_trust_servicer` (mismo datasource ya conectado), replicando el patrón interno de `CreateDistribution.apply()` del motor real, en vez de llamar a su endpoint HTTP público (el que usan integradores externos como scrapy-lambdas/JTP/Rapicredit/Solvento vía M2M) — decisión explícita del usuario, dado que este repo comparte datasource y está pensado como motor de cómputo+persistencia.
- `MasterServicerDistributionEntity` (antes solo lectura) se extiende con `firstPaymentDate`/`lastPaymentDate`/`creationDate`/`lastUpdateDate` y una relación `@OneToMany`/`@JoinTable` hacia `distribution_assignments`. Nueva `AssignmentEntity` (escritura, cascadeada).
- Nuevo `PersistDistributionUseCase`: `status=CALCULATED`/`NOTHING_DISTRIBUTABLE` según haya o no `Assignment`s; `currency` del deal; `concept` de la descripción de la regla o el owner; `first/last_payment_date` derivados de los fondos distribuibles (fallback a la fecha de corrida si el pool quedó vacío, ya que esas columnas son `NOT NULL` en la tabla real). Transaccional sobre `masterServicerTransactionManager`.
- Nuevo `MarkPaymentTapesAsDistributedUseCase`: marca `payment_tape.distribution_id` solo para los fondos `distributable` (nunca `ownerless`). Transaccional sobre `paymentsTransactionManager` — datasource distinto al de la distribución, sin transacción distribuida entre ambos (si la distribución se graba bien pero el marcado falla después, la distribución ya persistida no se revierte; es recuperable porque esos payment tapes seguirían siendo candidatos en la próxima corrida, sin duplicar la distribución gracias al check de VPR-9661).
- Gaps resueltos en el camino para que `Assignment` fuera persistible: `PoolFund` ganó `paymentDate` (se perdía al colapsar el pool) y `Assignment` ganó `concept` (no referencia la regla que lo originó).
- `RunDistributionUseCase` ahora persiste y marca al final de una corrida lista para distribuir; `DistributionExecutionResult` expone el `distributionId` real generado.
- Tests: `PersistDistributionUseCaseTest`, `MarkPaymentTapesAsDistributedUseCaseTest` (nuevos, Mockito/AAA) + un caso `@DataJpaTest` en `MasterServicerDistributionJPARepositoryTest` verificando el cascade real hacia `distribution_assignments` contra H2 + toda la suite existente actualizada. Corridos vía `./scripts/run-tests.sh`, confirmados pasando por el usuario.

### Phases Completed
- [x] **Phase 1: `PoolFund.paymentDate` y `Assignment.concept`** — campos agregados y propagados, tests existentes actualizados.
- [x] **Phase 2: Persistencia de Distribution + Assignment** — entities de escritura + `PersistDistributionUseCase`, con tests unitarios y de integración.
- [x] **Phase 3: Marcado de payment tapes + wiring final** — `MarkPaymentTapesAsDistributedUseCase` + wiring completo en `RunDistributionUseCase`.
