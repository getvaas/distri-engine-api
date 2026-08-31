**Created at**: 2026-08-31
**Status**: Approved
**Based on story**: @specs/20260831-151429_review-activate-deactivate-config-vpr-9641/story.md

# Plan: Agregar capacidad de desactivar una config

### Goal
Agregar `DeactivateDistributionConfigUseCase` y su endpoint `POST /configs/{id}/deactivate`,
contraparte directa de `ActivateDistributionConfigUseCase` ya implementado.

### Context
- `src/main/java/.../application/usecase/ActivateDistributionConfigUseCase.java` — patrón exacto
  a replicar (sin la lógica de "hermanos", que es específica de activate).
- `src/main/java/.../infrastructure/web/DistributionConfigRouter.java` — patrón del endpoint
  `POST /configs/{id}/activate` a replicar.
- `src/test/java/.../application/usecase/ActivateDistributionConfigUseCaseTest.java` — patrón de
  test a replicar.
- `docs/architecture/distribution-config-schema.md` — tabla de endpoints, agregar la fila nueva.

### Public Contracts
- **Services**: `DeactivateDistributionConfigUseCase.execute(String id): DistributionConfig`
  (nuevo) — busca entity por id (lanza `DistributionConfigNotFoundException` si no existe), setea
  `status=INACTIVE`, `updatedAt=now`, guarda y devuelve el domain. Sin validar el status actual,
  sin lógica de hermanos.
- **Endpoint**: `POST /configs/{id}/deactivate` en `DistributionConfigRouter` (`@VaasSecurity`,
  mismo patrón que `/activate`).
- **Tests**: `DeactivateDistributionConfigUseCaseTest` (nuevo) —
  - `execute_activeConfig_setsStatusInactive`
  - `execute_alreadyInactiveConfig_isIdempotent`
  - `execute_draftConfig_setsStatusInactiveWithoutError`
  - `execute_configNotFound_throwsDistributionConfigNotFoundException`

### Phases

#### Phase 1: Use case + endpoint
- [ ] `DeactivateDistributionConfigUseCase` (nuevo, mismo paquete que `ActivateDistributionConfigUseCase`)
- [ ] `POST /configs/{id}/deactivate` en `DistributionConfigRouter`
- [ ] Inyección de `DeactivateDistributionConfigUseCase` en el router

#### Phase 2: Tests + Docs
- [ ] `execute_activeConfig_setsStatusInactive`
- [ ] `execute_alreadyInactiveConfig_isIdempotent`
- [ ] `execute_draftConfig_setsStatusInactiveWithoutError`
- [ ] `execute_configNotFound_throwsDistributionConfigNotFoundException`
- [ ] `docs/architecture/distribution-config-schema.md` — agregar
      `POST /configs/{id}/deactivate` a la tabla de endpoints

### Next Step
Completar Phase 1 (use case + endpoint) y confirmar que el proyecto compila antes de escribir los
tests.
