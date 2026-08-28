**Created at**: 2026-08-27
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9714
**Plan implemented**: @specs/20260827-205120_transfer-instructions-owner-metadata-namespace-vpr-9714/plan.md

# Story: Agregar namespace de metadata a cada assignment de Transfer Instructions

### Description
Cada owner asignado a una plantilla de instrucción de transferencia (VPR-9713) puede necesitar,
además, un valor de metadata específico para el proceso de distribución — por ejemplo
`metadata.amount`, que en ejecución se concatena con el `template_code` del owner para resolver
un dato puntual de esa instrucción. Hoy la lista de assignments solo guarda el código de plantilla;
esta historia le agrega ese campo de metadata (`namespace`) a cada assignment individual.

### Acceptance Criteria
- [x] **Given** un assignment con `ownerTemplateCode` y `namespace` completos, **When** se guarda
  la config, **Then** persiste tal cual.
- [x] **Given** un assignment sin `namespace` (null o vacío), **When** se intenta guardar, **Then**
  se rechaza — `namespace` es obligatorio en cada assignment.
- [x] **Given** una lista de assignments con `ownerTemplateCode` repetido dentro del mismo
  registro, **When** se intenta guardar, **Then** se rechaza — misma regla ya establecida en
  VPR-9713, ahora aplicada sobre el `ownerTemplateCode` de cada assignment.
- [x] **Given** distintos assignments con el mismo `namespace` (ej. varios owners usando
  `metadata.amount`), **When** se guarda, **Then** no hay conflicto — `namespace` no es único, solo
  `ownerTemplateCode` lo es.
- [x] **Given** una lista vacía o no enviada, **When** se guarda, **Then** persiste sin error —
  mismo comportamiento que VPR-9713.

### Additional Context
Cambia la forma del nodo `transferInstructions` ya existente (VPR-9713) — no agrega un nodo nuevo.
`TransferInstructionsConfig.ownerTemplateCodes: List<String>` pasa a
`TransferInstructionsConfig.assignments: List<TransferInstructionAssignment>`, con
`TransferInstructionAssignment(ownerTemplateCode, namespace)`.

`namespace` es un string libre (ej. `metadata.amount`) — el ticket no define un catálogo cerrado de
valores válidos. El propósito real del campo — concatenar `namespace` + `template_code` para
resolver metadata en tiempo de distribución — es responsabilidad de la etapa de **ejecución**
(Pista B), fuera de alcance de `distri-engine-api`: esta historia solo tipa y persiste el string.

Por ser un cambio de forma sobre un nodo existente, alcanza (y requiere) actualizar
`UpdateTransferInstructionsUseCase`, su DTO y los tests ya escritos en VPR-9713 para reflejar la
nueva forma — no es un endpoint nuevo.

Relacionado, explícitamente fuera de esta historia: **VPR-9715** (Carga de templates), que guarda
`templateId` al dar de alta documentos — no toca esta lista de assignments.
