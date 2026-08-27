**Created at**: 2026-08-27
**Status**: Done
**Based on story**: @specs/20260827-173307_transfer-instructions-owner-assignment-vpr-9713/story.md

# Plan: Asignar templates de instrucción de transferencia por owner

### Goal
Agregar un 9º nodo `transferInstructions` a `DistributionConfigPayload` que declara, por deal, qué
`templateOwnerCode` están asignados — sin duplicar los datos del `owner_dictionary.json` externo,
que sigue siendo la única fuente de verdad para cuentas, montos y regla de balance por owner.

### Context
- `src/main/java/.../domain/model/DistributionRulesConfig.java` y `ComponentOwnerRule.java` — el
  nodo tipado más simple del proyecto (record inmutable con una sola lista), sirve de plantilla
  directa para `TransferInstructionsConfig`.
- `src/main/java/.../application/usecase/UpdateDistributionRulesUseCase.java` — patrón exacto a
  replicar: busca entity, mapea a domain, reconstruye el payload completo, valida, serializa,
  guarda.
- `src/main/java/.../domain/model/DistributionConfigPayload.java` — hoy 9 campos
  (country, currency, pool, paymentFilters, virtualColumns, rules, ownership, readinessChecks,
  notifications); gana `transferInstructions` como 10º.
- `src/main/java/.../infrastructure/web/DistributionConfigRouter.java` — patrón de los `PUT`
  existentes (`@VaasSecurity`, `@PutMapping("/{id}/<nodo>")`), agrega
  `PUT /configs/{id}/transfer-instructions`.
- `src/main/java/.../application/usecase/InvalidDistributionConfigException.java` — se reusa para
  el caso de duplicados, mismo patrón que `UpdateDistributionRulesUseCase`.
- `docs/architecture/distribution-config-schema.md` — pasa de documentar 8 a 9 nodos.

### Public Contracts
- **Domain**: `TransferInstructionsConfig(List<String> templateOwnerCodes)` —
  `@JsonIgnoreProperties(ignoreUnknown = true)`.
- **DTO**: `UpdateTransferInstructionsRequest(List<String> templateOwnerCodes)`.
- **Services**: `UpdateTransferInstructionsUseCase.execute(String id, UpdateTransferInstructionsRequest request): DistributionConfig`.
- **Endpoint**: `PUT /configs/{id}/transfer-instructions` en `DistributionConfigRouter`.
- **Tests**: `UpdateTransferInstructionsUseCaseTest` —
  - `execute_noDuplicates_persistsList`
  - `execute_duplicateTemplateOwnerCode_throwsInvalidDistributionConfigException`
  - `execute_emptyList_persistsEmptyList`
  - `execute_nullList_persistsEmptyList`
  - `execute_updatesTransferInstructions_preservesRestOfPayload`

### Phases

#### Phase 1: Modelo tipado
- [x] `TransferInstructionsConfig` (domain, `domain/model/`)
- [x] Campo `transferInstructions` nuevo en `DistributionConfigPayload` (10º parámetro)
- [x] `UpdateTransferInstructionsRequest` (DTO, `infrastructure/web/dto/`)

#### Phase 2: Use case + validación
- [x] `UpdateTransferInstructionsUseCase` — mismo flujo que `UpdateDistributionRulesUseCase`:
      busca entity por id, mapea a domain, reconstruye `DistributionConfigPayload` completo
      (10 campos, resto de `existing.config()` sin tocar), serializa, guarda
- [x] Lista `null` o vacía → persiste `List.of()`, sin error
- [x] `templateOwnerCode` repetido dentro de la misma lista →
      `InvalidDistributionConfigException`
- [x] Sin unicidad global entre distintos registros — no se valida contra otras configs

#### Phase 3: Endpoint
- [x] `PUT /configs/{id}/transfer-instructions` en `DistributionConfigRouter`
      (`@VaasSecurity`, mismo patrón que los demás `PUT` por nodo)
- [x] Inyección de `UpdateTransferInstructionsUseCase` en el router

#### Phase 4: Tests
- [x] `execute_noDuplicates_persistsList` — lista sin duplicados persiste tal cual
- [x] `execute_duplicateTemplateOwnerCode_throwsInvalidDistributionConfigException`
- [x] `execute_emptyList_persistsEmptyList`
- [x] `execute_nullList_persistsEmptyList`
- [x] `execute_updatesTransferInstructions_preservesRestOfPayload` — no pisa pool/ownership/etc.
- [x] `lenient()` en el stub de `repository.save(entity)` dentro de `mockExisting()` (mismo
      patrón ya aplicado en los 6 test files existentes, evita `UnnecessaryStubbingException`)

#### Phase 5: Docs
- [x] `docs/architecture/distribution-config-schema.md` — tabla "Los 8 nodos" pasa a 9, agrega
      fila `Transfer Instructions` con ticket VPR-9713
- [x] Sección de detalle del nodo `transferInstructions` (estructura + nota de unicidad
      por-registro, no global)
- [x] Tabla "Endpoints por nodo" — agrega `PUT /configs/{id}/transfer-instructions`

### Next Step
Completar Phase 1 (modelo tipado) y correr los tests existentes para confirmar que
`DistributionConfigPayload` con el nuevo campo no rompe ningún mapper/test ya construido.
