**Created at**: 2026-08-28
**Status**: Approved
**Based on story**: @specs/20260828-141521_distribution-rules-component-owner-toggle-vpr-9699/story.md

# Plan: Habilitar o deshabilitar la asignación de owner por componente de cuota

### Goal
Agregar el toggle `hasComponentOwners` a `DistributionRulesConfig` para que un deal declare
explícitamente si usa o no la asignación de owner por componente de cuota, sin forzar consistencia
con `componentOwners` al guardar.

### Context
- `src/main/java/.../domain/model/DistributionRulesConfig.java` — modelo actual a extender.
- `src/main/java/.../infrastructure/web/dto/UpdateDistributionRulesRequest.java` — DTO actual:
  `UpdateDistributionRulesRequest(List<ComponentOwnerRuleRequest> componentOwners)`.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` — construye
  `DistributionRulesConfig` reconstruyendo el payload completo; solo cambia el constructor de
  `DistributionRulesConfig`, no la lógica de validación de `ComponentOwnerRule` (component/owner
  obligatorios, sin duplicados).
- No se toca `DistributionConfigRouter.java` — mismo endpoint `PUT /configs/{id}/distribution-rules`.
- `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" a actualizar.

### Public Contracts
- **Domain**: `DistributionRulesConfig(boolean hasComponentOwners, List<ComponentOwnerRule> componentOwners)`
  (modificado).
- **DTO**: `UpdateDistributionRulesRequest(Boolean hasComponentOwners, List<ComponentOwnerRuleRequest> componentOwners)`
  (modificado).
- **Services**: `UpdateDistributionRulesUseCase.execute(String id, UpdateDistributionRulesRequest request): DistributionConfig`
  — misma firma.
- **Tests**: extender `UpdateDistributionRulesUseCaseTest` —
  - `execute_hasComponentOwnersTrue_persists`
  - `execute_hasComponentOwnersFalseWithEmptyList_persists`
  - `execute_hasComponentOwnersFalseWithData_persistsWithoutError`
  - `execute_hasComponentOwnersNotSent_defaultsToFalse`

### Phases

#### Phase 1: Modelo + DTO
- [ ] `DistributionRulesConfig` gana `hasComponentOwners: boolean` (primer parámetro)
- [ ] `UpdateDistributionRulesRequest` gana `hasComponentOwners: Boolean`

#### Phase 2: Use case
- [ ] `UpdateDistributionRulesUseCase.buildDistributionRulesConfig` pasa `hasComponentOwners` al
      construir `DistributionRulesConfig`
- [ ] `hasComponentOwners` no enviado (`null`) → `false` por default
- [ ] Sin validación cruzada con `componentOwners` — cualquier combinación se persiste tal cual
- [ ] Validaciones existentes de `ComponentOwnerRule` (component/owner obligatorios, sin
      duplicados) sin cambios

#### Phase 3: Tests + Docs
- [ ] `execute_hasComponentOwnersTrue_persists`
- [ ] `execute_hasComponentOwnersFalseWithEmptyList_persists`
- [ ] `execute_hasComponentOwnersFalseWithData_persistsWithoutError`
- [ ] `execute_hasComponentOwnersNotSent_defaultsToFalse`
- [ ] `docs/architecture/distribution-config-schema.md` — sección "Distribution Rules" actualizada
      con `hasComponentOwners`

### Next Step
Completar Phase 1 (modelo + DTO) y confirmar que el proyecto compila con el nuevo campo antes de
tocar el use case.
