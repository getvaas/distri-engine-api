**Created at**: 2026-08-21
**Status**: Done
**Based on story**: @specs/20260821-154114_readiness-checks-engine-business-day-vpr-9661/story.md

# Plan: Motor de Readiness Checks + check de día hábil

### Goal
Tipar la sección `readinessChecks` del payload (config) y construir un motor de checks extensible
(ejecución) con una sola implementación real (día hábil) — los otros 2 checks quedan enchufables sin
rediseño futuro.

### Context
- `distri-engine-api/.../DistributionConfigPayload.java` — `readinessChecks` pasa de `JsonNode` a
  `ReadinessChecksConfig`, mismo patrón incremental que `pool` (VPR-9628).
- `distri-engine-api/.../ResolveActiveDistributionConfigUseCase.java` (VPR-9660) — el nuevo use case de
  ejecución lo reusa para saber contra qué config correr los checks.
- `conciliation-engine-api/.../domain/service/matcher/` — mismo patrón de familia de estrategias
  intercambiables (interfaz + `@Component` por implementación) que se usa acá para los checks.
- `docs/distribution-engine-onboarding.html` STEP 6 — mockup real de Preconditions + Failure behavior.

### Public Contracts
- **Domain**: `ReadinessCheckType`, `ReadinessCheckFailureAction`, `ReadinessCheckRetry`,
  `ReadinessCheckStatus` (enums); `ReadinessChecksConfig(enabledChecks, failureAction, retry)`;
  `ReadinessCheckContext(companyId, date, country)`; `ReadinessCheckResult(type, status, reason)`;
  `ReadinessCheckOutcome(readyToDistribute, results)`.
- **Domain service**: `ReadinessCheck` (interfaz), `BusinessDayCheck` (única implementación real),
  `ReadinessCheckRunner` (orquestador, marca `NOT_IMPLEMENTED` lo que no tiene bean registrado).
- **Use cases**: `UpdateReadinessChecksConfigUseCase`, `RunReadinessChecksUseCase`.
- **Endpoints**: `PUT /configs/{id}/readiness-checks`, `GET /configs/readiness?companyId=&date=`.
- **Tests**: `UpdateReadinessChecksConfigUseCaseTest`, `RunReadinessChecksUseCaseTest`,
  `BusinessDayCheckTest`, `ReadinessCheckRunnerTest`.

### Phases

#### Phase 1: Config tipada
[Reemplaza el placeholder JsonNode por la estructura real de Preconditions + Failure behavior.]
- [x] Enums (`ReadinessCheckType`, `ReadinessCheckFailureAction`, `ReadinessCheckRetry`)
- [x] `ReadinessChecksConfig` + `DistributionConfigPayload.readinessChecks` tipado
- [x] `UpdateReadinessChecksConfigUseCase` (defaults: 3 checks + PAUSE_AND_ALERT + NEXT_CYCLE)
- [x] Endpoint `PUT /configs/{id}/readiness-checks`

#### Phase 2: Motor de ejecución
[El runner + la única implementación real, diseñados para que agregar checks después no toque el runner.]
- [x] `ReadinessCheck` (interfaz), `ReadinessCheckContext`, `ReadinessCheckResult`, `ReadinessCheckStatus`
- [x] `BusinessDayCheck` (fin de semana; TODO calendario de feriados por país)
- [x] `ReadinessCheckRunner` (marca `NOT_IMPLEMENTED` lo no registrado)
- [x] `RunReadinessChecksUseCase` + `ReadinessCheckOutcome`
- [x] Endpoint `GET /configs/readiness?companyId=&date=`

#### Phase 3: Tests
[Cubre config con defaults, el check real en sus 2 estados, y el caso NOT_IMPLEMENTED.]
- [x] Config: defaults, valores custom
- [x] `BusinessDayCheck`: fin de semana falla, día de semana pasa
- [x] `ReadinessCheckRunner`: check no registrado → NOT_IMPLEMENTED
- [x] `RunReadinessChecksUseCase`: agrega resultados, decide `readyToDistribute`

### Next Step
Completado — Payment tape cargado (VPR-9662+) y No duplicate distribution (sin ticket todavía) se
agregan después como nuevos beans `ReadinessCheck`, sin tocar el runner.
