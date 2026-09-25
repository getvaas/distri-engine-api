**Created at**: 2026-09-25
**Status**: In Progress
**Based on story**: @story.md

# Plan: Ejecutar una distribución en modo borrador, revisable antes de confirmarse

### Goal
Agregar un modo borrador (`draftModeEnabled`) por deal: si está habilitado, `RunDistributionUseCase` persiste con `status=DRAFT`, marca los payment tapes igual que siempre, pero no notifica. Un endpoint nuevo aprueba una distribución en `DRAFT` (mismo id, sin recalcular montos) y recién ahí dispara la notificación. De paso, alinea `PersistDistributionUseCase` a los valores reales de `status` (`approved`/`nothing_distributable`/`draft`) en vez de los inventados (`CALCULATED`/`NOTHING_DISTRIBUTABLE`).

### Context
- `DistributionConfigPayload`/`CreateDistributionConfigRequest`/`UpdateDistributionConfigRequest` tienen los campos sueltos `country`/`currency` a nivel raíz (no dentro de ningún nodo del wizard) — `draftModeEnabled` va ahí, mismo patrón.
- `UpdateDistributionConfigUseCase` ya tiene el patrón de fallback para campos sueltos (`request.currency() != null ? request.currency() : existing.config().currency()`) — `draftModeEnabled` sigue el mismo patrón.
- `PersistDistributionUseCase` hoy usa `STATUS_CALCULATED`/`STATUS_NOTHING_DISTRIBUTABLE` (strings inventados) — se reemplazan por un enum `DistributionStatus` con los valores reales confirmados contra el sistema real (`approved, not_enabled, cannot_distribute, nothing_distributable, draft, error`); solo `approved`/`nothing_distributable`/`draft` son alcanzables desde este motor hoy.
- `NotifyDistributionResultUseCase.execute(DistributionConfig, Long companyId, DistributionExecutionResult)` está atado al shape de `RunDistributionUseCase` (necesita `readiness`/`funds` que no existen al aprobar un draft ya persistido) — se cambia su firma a los datos mínimos que realmente usa: `(DistributionConfig, Long companyId, Long distributionId, int assignmentsCount)`.
- `MasterServicerDistributionEntity` ya tiene `status: String` y `assignments: List<AssignmentEntity>` — alcanza con re-setear `status` y volver a guardar (JPA merge), sin tocar assignments.
- `NoDuplicateDistributionCheck` sigue igual (filtra por `active`+fecha, no por status) — una distribución DRAFT ya bloquea una segunda corrida ese día, comportamiento aceptado, no se toca.

### Public Contracts
- **Domain**:
  - `domain/model/enums/DistributionStatus.java` (nuevo) — `APPROVED, NOT_ENABLED, CANNOT_DISTRIBUTE, NOTHING_DISTRIBUTABLE, DRAFT, ERROR`; se persiste como `name().toLowerCase()` (matchea exacto los valores reales).
  - `DistributionConfigPayload` — nuevo campo `Boolean draftModeEnabled` (raíz, junto a `country`/`currency`).
- **Infrastructure/web**:
  - `CreateDistributionConfigRequest`/`UpdateDistributionConfigRequest`/`DistributionConfigResponse` — nuevo campo `Boolean draftModeEnabled`, mismo nivel que `country`/`currency`.
  - `ApproveDraftDistributionRequest(Long companyId)` (nuevo DTO).
  - `DistributionExecutionRouter` — nuevo `POST /distributions/{id}/approve`, `@VaasSecurity`, body `ApproveDraftDistributionRequest`.
- **Application**:
  - `CreateDistributionConfigUseCase`/`UpdateDistributionConfigUseCase` — wirean `draftModeEnabled` (update con el mismo patrón de fallback que `currency`).
  - `PersistDistributionUseCase` — `status = assignments.isEmpty() ? NOTHING_DISTRIBUTABLE : (config.config().draftModeEnabled() == true ? DRAFT : APPROVED)`.
  - `RunDistributionUseCase` — llama a `notifyDistributionResultUseCase` solo si el status persistido no es `DRAFT`.
  - `NotifyDistributionResultUseCase` — firma nueva `execute(DistributionConfig config, Long companyId, Long distributionId, int assignmentsCount)`.
  - `ApproveDraftDistributionUseCase.execute(Long distributionId, Long companyId)` (nuevo): busca la distribución por id; si no existe → `MasterServicerDistributionNotFoundException` (404); si `status != DRAFT` → `DistributionNotInDraftStatusException` (409); si está OK, `status = APPROVED`, guarda, resuelve la config activa de `companyId` y llama a `notifyDistributionResultUseCase` con el id y la cantidad de assignments ya persistidos.
- **Tests**: `PersistDistributionUseCaseTest` (casos `draftModeEnabled=true/false/null`, valores de status reales), `RunDistributionUseCaseTest` (no notifica en draft, sí notifica en el flujo normal), `NotifyDistributionResultUseCaseTest` (ajustar a la firma nueva), `ApproveDraftDistributionUseCaseTest` (aprueba OK, 404, 409), `CreateDistributionConfigUseCaseTest`/`UpdateDistributionConfigUseCaseTest` (wiring del flag, fallback en update).

### Phases

#### Phase 1: Enum de status real + flag de config + PersistDistributionUseCase alineado
- [x] Crear `domain/model/enums/DistributionStatus.java`.
- [x] Agregar `draftModeEnabled` a `DistributionConfigPayload`, `CreateDistributionConfigRequest`, `UpdateDistributionConfigRequest`, `DistributionConfigResponse`.
- [x] Wirear el flag en `CreateDistributionConfigUseCase`/`UpdateDistributionConfigUseCase` (fallback en update).
- [x] Reescribir `PersistDistributionUseCase` para usar `DistributionStatus` (valores reales, rama `DRAFT`).
- [x] Tests de las piezas de arriba.

#### Phase 2: Saltar notificación en draft + endpoint de aprobación
- [x] `RunDistributionUseCase`: no notificar si el status persistido es `DRAFT`.
- [x] Refactor de firma de `NotifyDistributionResultUseCase.execute(...)`.
- [x] `ApproveDraftDistributionUseCase` (nuevo) + excepciones `MasterServicerDistributionNotFoundException`/`DistributionNotInDraftStatusException` + mapeo en `GlobalExceptionHandler`.
- [x] `POST /distributions/{id}/approve` en `DistributionExecutionRouter` + `ApproveDraftDistributionRequest`.
- [x] Tests de todo lo anterior.

### Next Step
Ambas fases implementadas y compilando limpio. Falta correr `./scripts/run-tests.sh` y probar manualmente antes de marcar Status Done.
