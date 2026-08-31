**Created at**: 2026-08-31
**Status**: Done
**Based on story**: @specs/20260831-151429_review-activate-deactivate-config-vpr-9641/story.md

# Plan: Activar o desactivar una config mediante un único endpoint

### Goal
Reemplazar los endpoints separados `POST /configs/{id}/activate` /
`POST /configs/{id}/deactivate` por un único `PUT /configs/{id}/status`, recibiendo el status
deseado (`ACTIVE` o `INACTIVE`) en el body.

### Corrección de diseño (2026-08-31)
La primera versión de esta historia implementó dos endpoints separados
(`ActivateDistributionConfigUseCase` ya existente + `DeactivateDistributionConfigUseCase` nuevo).
Antes de mergear, se decidió unificarlos en uno solo — `DeactivateDistributionConfigUseCase` nunca
llegó a mergearse; se descartó junto con `ActivateDistributionConfigUseCase` en favor de
`UpdateDistributionConfigStatusUseCase`.

### Context
- `src/main/java/.../application/usecase/UpdateDistributionConfigStatusUseCase.java` — reemplaza a
  `ActivateDistributionConfigUseCase` (eliminado) y `DeactivateDistributionConfigUseCase`
  (eliminado, nunca mergeado).
- `src/main/java/.../infrastructure/web/DistributionConfigRouter.java` — reemplaza
  `POST /activate` + `POST /deactivate` por `PUT /status`.
- `src/main/java/.../application/usecase/MultipleActiveDistributionConfigException.java` —
  javadoc actualizado para referenciar el use case nuevo.
- `docs/architecture/distribution-config-schema.md` — sección de endpoint de estado reescrita.

### Public Contracts
- **DTO**: `UpdateDistributionConfigStatusRequest(DistributionConfigStatus status)` (nuevo).
- **Services**: `UpdateDistributionConfigStatusUseCase.execute(String id, UpdateDistributionConfigStatusRequest request): DistributionConfig`
  (nuevo) — busca entity (lanza `DistributionConfigNotFoundException` si no existe); `status=null`
  o `status=DRAFT` → `InvalidDistributionConfigException`; `status=ACTIVE` → desactiva cualquier
  otra config `ACTIVE` del mismo `companyId` (misma lógica que tenía
  `ActivateDistributionConfigUseCase`); `status=INACTIVE` → solo setea, sin efectos secundarios,
  idempotente.
- **Endpoint**: `PUT /configs/{id}/status` en `DistributionConfigRouter` (`@VaasSecurity`).
- **Tests**: `UpdateDistributionConfigStatusUseCaseTest` (nuevo, reemplaza a
  `ActivateDistributionConfigUseCaseTest` y `DeactivateDistributionConfigUseCaseTest`) —
  - `execute_activateWithNoOtherActiveConfig_setsActive`
  - `execute_activateWithAnotherConfigAlreadyActive_deactivatesTheOtherOne`
  - `execute_deactivateActiveConfig_setsInactiveWithoutTouchingSiblings`
  - `execute_deactivateAlreadyInactiveConfig_isIdempotent`
  - `execute_statusNull_throwsInvalidDistributionConfigException`
  - `execute_statusDraft_throwsInvalidDistributionConfigException`
  - `execute_configNotFound_throwsDistributionConfigNotFoundException`

### Phases

#### Phase 1: Use case + endpoint
- [x] `UpdateDistributionConfigStatusRequest` (DTO, nuevo)
- [x] `UpdateDistributionConfigStatusUseCase` (nuevo)
- [x] Eliminar `ActivateDistributionConfigUseCase` y `DeactivateDistributionConfigUseCase`
- [x] `PUT /configs/{id}/status` en `DistributionConfigRouter`, reemplaza `/activate` +
      `/deactivate`

#### Phase 2: Tests + Docs
- [x] `execute_activateWithNoOtherActiveConfig_setsActive`
- [x] `execute_activateWithAnotherConfigAlreadyActive_deactivatesTheOtherOne`
- [x] `execute_deactivateActiveConfig_setsInactiveWithoutTouchingSiblings`
- [x] `execute_deactivateAlreadyInactiveConfig_isIdempotent`
- [x] `execute_statusNull_throwsInvalidDistributionConfigException`
- [x] `execute_statusDraft_throwsInvalidDistributionConfigException`
- [x] `execute_configNotFound_throwsDistributionConfigNotFoundException`
- [x] Eliminar `ActivateDistributionConfigUseCaseTest` y `DeactivateDistributionConfigUseCaseTest`
- [x] `docs/architecture/distribution-config-schema.md` — endpoint de estado unificado

### Next Step
Ninguno — historia completa.
