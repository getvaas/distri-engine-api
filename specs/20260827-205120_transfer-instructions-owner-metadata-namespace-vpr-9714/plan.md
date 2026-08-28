**Created at**: 2026-08-27
**Status**: Done
**Based on story**: @specs/20260827-205120_transfer-instructions-owner-metadata-namespace-vpr-9714/story.md

# Plan: Agregar namespace de metadata a cada assignment de Transfer Instructions

### Goal
Reestructurar `TransferInstructionsConfig` de `List<String> ownerTemplateCodes` a
`List<TransferInstructionAssignment>`, agregando un campo `namespace` obligatorio por assignment
para el matching de metadata en tiempo de distribución.

### Context
- `src/main/java/.../domain/model/TransferInstructionsConfig.java` — modelo actual de VPR-9713 a
  reestructurar.
- `src/main/java/.../application/usecase/UpdateTransferInstructionsUseCase.java` — reescribir
  `buildTransferInstructionsConfig`.
- `src/main/java/.../infrastructure/web/dto/UpdateTransferInstructionsRequest.java` — cambia de
  `List<String>` a `List<TransferInstructionAssignmentRequest>`.
- `src/main/java/.../domain/model/ComponentOwnerRule.java` /
  `infrastructure/web/dto/ComponentOwnerRuleRequest.java` — patrón a replicar para el nuevo record
  anidado con validación por-item.
- `src/test/java/.../application/usecase/UpdateTransferInstructionsUseCaseTest.java` — reescritura
  completa (ya escrito en VPR-9713, forma vieja).
- `docs/architecture/distribution-config-schema.md` — sección "Transfer Instructions" a reescribir.
- No se toca `DistributionConfigRouter.java` ni `DistributionConfigPayload.java` — el campo
  `transferInstructions` ya existe, solo cambia su tipo interno.

### Public Contracts
- **Domain**: `TransferInstructionAssignment(String ownerTemplateCode, String namespace)` (nuevo);
  `TransferInstructionsConfig(List<TransferInstructionAssignment> assignments)` (modificado).
- **DTO**: `TransferInstructionAssignmentRequest(String ownerTemplateCode, String namespace)`
  (nuevo); `UpdateTransferInstructionsRequest(List<TransferInstructionAssignmentRequest> assignments)`
  (modificado).
- **Services**: `UpdateTransferInstructionsUseCase.execute(String id, UpdateTransferInstructionsRequest request): DistributionConfig`
  — misma firma, `buildTransferInstructionsConfig` reescrito.
- **Tests**: `UpdateTransferInstructionsUseCaseTest` (reescritura completa) —
  - `execute_validAssignments_persistsList`
  - `execute_missingNamespace_throwsInvalidDistributionConfigException`
  - `execute_blankNamespace_throwsInvalidDistributionConfigException`
  - `execute_duplicateTemplateOwnerCode_throwsInvalidDistributionConfigException`
  - `execute_repeatedNamespaceAcrossAssignments_persistsWithoutConflict`
  - `execute_emptyList_persistsEmptyList`
  - `execute_nullList_persistsEmptyList`
  - `execute_updatesTransferInstructions_preservesRestOfPayload`

### Phases

#### Phase 1: Modelo tipado
- [x] `TransferInstructionAssignment` (domain, nuevo)
- [x] `TransferInstructionsConfig` — `ownerTemplateCodes: List<String>` → `assignments: List<TransferInstructionAssignment>`
- [x] `TransferInstructionAssignmentRequest` (DTO, nuevo)
- [x] `UpdateTransferInstructionsRequest` — `ownerTemplateCodes: List<String>` → `assignments: List<TransferInstructionAssignmentRequest>`

#### Phase 2: Use case + validación
- [x] `UpdateTransferInstructionsUseCase.buildTransferInstructionsConfig` mapea
      `TransferInstructionAssignmentRequest` → `TransferInstructionAssignment`
- [x] `namespace` null o blank → `InvalidDistributionConfigException`
- [x] `ownerTemplateCode` repetido dentro de la misma lista → `InvalidDistributionConfigException`
      (mismo patrón que VPR-9713, ahora sobre `assignment.ownerTemplateCode()`)
- [x] `namespace` repetido entre distintos assignments → sin validación, es válido
- [x] Lista `null` o vacía → persiste `List.of()`, sin error

#### Phase 3: Tests
- [x] `execute_validAssignments_persistsList`
- [x] `execute_missingNamespace_throwsInvalidDistributionConfigException`
- [x] `execute_blankNamespace_throwsInvalidDistributionConfigException`
- [x] `execute_duplicateTemplateOwnerCode_throwsInvalidDistributionConfigException`
- [x] `execute_repeatedNamespaceAcrossAssignments_persistsWithoutConflict`
- [x] `execute_emptyList_persistsEmptyList`
- [x] `execute_nullList_persistsEmptyList`
- [x] `execute_updatesTransferInstructions_preservesRestOfPayload`

#### Phase 4: Docs
- [x] Reescribir sección "Transfer Instructions" en `distribution-config-schema.md` con la forma
      nueva (`assignments: [{ownerTemplateCode, namespace}]`)

### Next Step
Completar Phase 1 (modelo tipado) y confirmar que el proyecto compila con la nueva forma antes de
tocar el use case.
