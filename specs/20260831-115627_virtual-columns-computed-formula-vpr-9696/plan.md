**Created at**: 2026-08-31
**Status**: Done
**Based on story**: @specs/20260831-115627_virtual-columns-computed-formula-vpr-9696/story.md

# Plan: Tipar Virtual Columns — columnas derivadas por fórmula sobre el payment tape

### Goal
Tipar el placeholder `virtualColumns: JsonNode` en `DistributionConfigPayload` como
`VirtualColumnsConfig`, con su propio endpoint de actualización, siguiendo el patrón ya
establecido para el resto de los nodos.

### Context
- `src/main/java/.../domain/model/DistributionConfigPayload.java` — `virtualColumns` cambia de
  `JsonNode` a `VirtualColumnsConfig`.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` — patrón exacto a
  replicar para el nuevo use case (busca entity, mapea, reconstruye payload completo, valida,
  serializa, guarda).
- `src/main/java/.../infrastructure/web/DistributionConfigRouter.java` — patrón de los `PUT`
  existentes, agrega `PUT /configs/{id}/virtual-columns`.
- `src/main/java/.../infrastructure/web/dto/DistributionConfigResponse.java` — hoy NO expone
  `virtualColumns` (quedó afuera cuando era placeholder) — se agrega en esta historia.
- Todos los demás use cases (`UpdatePoolConfigUseCase`, `UpdatePaymentFiltersUseCase`, etc.) solo
  hacen passthrough de `existing.config().virtualColumns()` al reconstruir el payload — el cambio
  de tipo no rompe esas llamadas, no requieren modificación.
- `docs/architecture/distribution-config-schema.md` — Virtual Columns pasa de "⏳ Placeholder" a
  "✅ Tipado" en la tabla de 9 nodos.

### Public Contracts
- **Domain**: `VirtualColumn(String name, String formula)` (nuevo).
  `VirtualColumnsConfig(List<VirtualColumn> columns)` (nuevo).
  `DistributionConfigPayload.virtualColumns: VirtualColumnsConfig` (cambio de tipo, mismo
  parámetro, ya no `JsonNode`).
- **DTO**: `VirtualColumnRequest(String name, String formula)` (nuevo).
  `UpdateVirtualColumnsRequest(List<VirtualColumnRequest> columns)` (nuevo).
- **Services**: `UpdateVirtualColumnsUseCase.execute(String id, UpdateVirtualColumnsRequest request): DistributionConfig`
  (nuevo) — valida `name`/`formula` obligatorios (no null/blank) y `name` sin duplicados dentro de
  `columns`; sin validación de ciclos/orden.
- **Endpoint**: `PUT /configs/{id}/virtual-columns` en `DistributionConfigRouter`.
- **Response**: `DistributionConfigResponse` gana `virtualColumns: VirtualColumnsConfig`.
- **Tests**: `UpdateVirtualColumnsUseCaseTest` (nuevo) —
  - `execute_columnsWithNameAndFormula_persistsAsIs`
  - `execute_columnWithoutName_throwsInvalidDistributionConfigException`
  - `execute_columnWithoutFormula_throwsInvalidDistributionConfigException`
  - `execute_duplicateName_throwsInvalidDistributionConfigException`
  - `execute_emptyOrMissingColumns_persistsEmptyListWithoutError`
  - `execute_formulaReferencingAnotherVirtualColumn_persistsWithoutError`
  - `execute_preservesRestOfPayload`

### Phases

#### Phase 1: Modelo tipado
- [x] `VirtualColumn` (domain, nuevo)
- [x] `VirtualColumnsConfig` (domain, nuevo)
- [x] `DistributionConfigPayload.virtualColumns` — `JsonNode` → `VirtualColumnsConfig`
- [x] `VirtualColumnRequest` (DTO, nuevo)
- [x] `UpdateVirtualColumnsRequest` (DTO, nuevo)

#### Phase 2: Use case + validación
- [x] `UpdateVirtualColumnsUseCase` — mismo flujo que `UpdateDistributionRulesUseCase`
- [x] `name` null/blank → `InvalidDistributionConfigException`
- [x] `formula` null/blank → `InvalidDistributionConfigException`
- [x] `name` repetido dentro de `columns` → `InvalidDistributionConfigException`
- [x] Lista `null`/vacía → persiste `List.of()`, sin error
- [x] Sin validación de anidamiento/ciclos/orden de evaluación

#### Phase 3: Endpoint
- [x] `PUT /configs/{id}/virtual-columns` en `DistributionConfigRouter` (`@VaasSecurity`, mismo
      patrón que los demás `PUT` por nodo)
- [x] Inyección de `UpdateVirtualColumnsUseCase` en el router
- [x] `DistributionConfigResponse` gana `virtualColumns: VirtualColumnsConfig`

#### Phase 4: Tests + Docs
- [x] `execute_columnsWithNameAndFormula_persistsAsIs`
- [x] `execute_columnWithoutName_throwsInvalidDistributionConfigException`
- [x] `execute_columnWithoutFormula_throwsInvalidDistributionConfigException`
- [x] `execute_duplicateName_throwsInvalidDistributionConfigException`
- [x] `execute_emptyOrMissingColumns_persistsEmptyListWithoutError`
- [x] `execute_formulaReferencingAnotherVirtualColumn_persistsWithoutError`
- [x] `execute_preservesRestOfPayload`
- [x] `docs/architecture/distribution-config-schema.md` — Virtual Columns pasa a "✅ Tipado",
      detalle del nodo, endpoint nuevo en la tabla de endpoints

### Next Step
Completar Phase 1 (modelo tipado) y confirmar que el proyecto compila con el cambio de tipo de
`virtualColumns` antes de tocar el use case.
