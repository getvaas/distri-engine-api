**Created at**: 2026-09-03
**Status**: Done
**Based on story**: @story.md

# Plan: Implementar los checks de Readiness pendientes (payment tape cargado, sin duplicados)

### Goal
Construir `PaymentTapeLoadedCheck` y `NoDuplicateDistributionCheck` para que el motor de Readiness
Checks (VPR-9661) deje de reportarlos `NOT_IMPLEMENTED`, y reservar el campo
`forceRunOnNonBusinessDay` en el schema para un override futuro de `BusinessDayCheck`.

### Context
- `src/main/java/com/getvaas/distribution/engine/domain/model/ReadinessCheckContext.java` — gana `masterTrustId` y `pool`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/RunReadinessChecksUseCase.java` — arma el context enriquecido.
- `src/main/java/com/getvaas/distribution/engine/domain/service/readiness/` — nuevos checks van acá, junto a `BusinessDayCheck`/`ReadinessCheckRunner`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/FetchCandidatePaymentTapesUseCase.java` — referencia de la lógica de ventana/candidatos a replicar (no a invocar directamente).
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/payments/PaymentTapeJPARepository.java` — repositorio existente que usa `PaymentTapeLoadedCheck`.
- `src/main/java/com/getvaas/distribution/engine/domain/service/calendar/WorkingDaysCalculator.java` — cálculo de ventana de días hábiles, ya usado por VPR-9662.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/masterservicer/` — nueva entity/repositorio de solo lectura sobre `distribution` van acá, junto a `DistributionConfigJPARepository`.
- `src/main/java/com/getvaas/distribution/engine/domain/model/ReadinessCheckSetting.java`, `infrastructure/web/dto/ReadinessCheckSettingRequest.java`, `application/usecase/ReadinessChecksConfigBuilder.java` — ganan `forceRunOnNonBusinessDay`.

### Public Contracts
- **Services**:
  - `ReadinessCheckContext(Long companyId, LocalDate date, String country, Long masterTrustId, PoolConfig pool)`
  - `PaymentTapeLoadedCheck.evaluate(context) -> ReadinessCheckResult` (type `PAYMENT_TAPE_LOADED`)
  - `NoDuplicateDistributionCheck.evaluate(context) -> ReadinessCheckResult` (type `NO_DUPLICATE_DISTRIBUTION`)
  - `MasterServicerDistributionJPARepository.existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(Long, LocalDateTime, LocalDateTime) -> boolean`
- **Database**: ninguna migración nueva — `distribution` ya existe en `master_trust_servicer`, mapeo de solo lectura (`hibernate.hbm2ddl.auto=none`, ya vigente para ese EntityManagerFactory).
- **Domain model**: `ReadinessCheckSetting(type, failureAction, retry, forceRunOnNonBusinessDay: Boolean)`.

### Phases

#### Phase 1: Enriquecer el context y reservar el campo de override
- [x] `ReadinessCheckContext` gana `masterTrustId` y `pool` (nullable donde no aplique).
- [x] `RunReadinessChecksUseCase` arma el context con `config.masterTrustId()` y `config.config().pool()`.
- [x] `ReadinessCheckSetting` gana `forceRunOnNonBusinessDay: Boolean` (nullable) + `ReadinessCheckSettingRequest` (DTO) + `ReadinessChecksConfigBuilder` lo persisten tal cual, sin validarlo ni usarlo. `BusinessDayCheck` no lo lee.
- [x] Actualizar `RunReadinessChecksUseCaseTest`, `ReadinessChecksConfigBuilderTest` y `BusinessDayCheckTest` (si existe) por los campos nuevos — deben seguir pasando sin cambiar su comportamiento.

#### Phase 2: PaymentTapeLoadedCheck
- [x] `PaymentTapeLoadedCheck implements ReadinessCheck` (`domain/service/readiness/`): si `context.pool() == null || pool.strategy() != PAYMENT_TAPE` → `PASSED` directo. Si es `PAYMENT_TAPE`: calcula la ventana con `WorkingDaysCalculator.subtractWorkingDays(date, daysBack, country)` (mismo default 90 si `daysBack` es null) y consulta `PaymentTapeJPARepository.findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull` — `PASSED` si la lista no está vacía, `FAILED` si está vacía.
- [x] `PaymentTapeLoadedCheckTest`: casos PAYMENT_TAPE con/sin candidatos, ACCOUNT_BALANCE y DATA_SOURCE_AGGREGATION (verificar que NO se consulta el repositorio en estos dos), pool null.

#### Phase 3: NoDuplicateDistributionCheck
- [x] `MasterServicerDistributionEntity` (`infrastructure/persistence/masterservicer/entity/`, `@Table(name = "distribution")`): `id (Long, autoincrement)`, `masterTrustServicerId (Long)`, `status (String)`, `distributionDate (LocalDateTime)`, `active (Boolean, nullable)`. Comentario dejando explícito que es de solo lectura, propiedad de `master-trust-servicer-api`.
- [x] `MasterServicerDistributionJPARepository extends JpaRepository<MasterServicerDistributionEntity, Long>` con `existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween`.
- [x] `MasterServicerDistributionJPARepositoryTest` (`@DataJpaTest`): seedea filas con la forma real compartida por el usuario (incluida una con `active` null), confirma que la query solo cuenta las `active=true` del `masterTrustServicerId`/fecha correctos.
- [x] `NoDuplicateDistributionCheck implements ReadinessCheck`: `FAILED` si `existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(context.masterTrustId(), inicio del día, fin del día)` es `true`; si no, `PASSED`. **Decisión revisada tras el plan inicial**: si `context.masterTrustId()` es null, también `FAILED` (no `PASSED`) — es una config incompleta, no un caso neutro; no se puede afirmar que no hay duplicado sin ese dato.
- [x] `NoDuplicateDistributionCheckTest` (Mockito): `FAILED`/`PASSED` según el repositorio mockeado, `masterTrustId` null → `FAILED`.
- [x] **Fuera del alcance original, agregado en esta misma iteración**: `RunReadinessChecksUseCase` rechaza (`DistributionConfigNotActiveException`, HTTP 409) cualquier config con `status != ACTIVE` — antes nada impedía correr readiness checks sobre una config `DRAFT`/incompleta.

### Next Step
All phases completed and verified with a real passing test run (`./scripts/run-tests.sh`). See `resume.md`.
