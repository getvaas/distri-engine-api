# Original Request

**Source**: Jira ticket [VPR-9714](https://pmvaas1.atlassian.net/browse/VPR-9714) — "Transfer Instructions: Creación de owner metadatos"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-27)

generar el matching clave valor de los metadatos y values para el matching de assigments.

En el distribution_engine_config en la seccion de transfer instruction, tendremos una lista de
assigments donde tendremos un campo mas que es el namespace del template_code

en el namespace podremos agregar un valor por ejemplo: metadata.amount
y se le concatenara el valor del template_code cuando se requiera por el proceso de distribucion.

## Context from VPR-9713 (already implemented, PR #6)

`TransferInstructionsConfig` hoy tiene `List<String> ownerTemplateCodes` — una referencia liviana
al `owner_dictionary.json` externo, con regla de unicidad de `ownerTemplateCode` por registro (no
global). VPR-9714 se construye directamente sobre esa lista.

## User additions (this session)

- Confirmado: el modelo se restructura de `List<String> ownerTemplateCodes` a
  `List<TransferInstructionAssignment> assignments`, con
  `TransferInstructionAssignment(String ownerTemplateCode, String namespace)`.
- Confirmado: `namespace` es obligatorio en cada assignment, no opcional.
- La regla de unicidad de `ownerTemplateCode` por registro (heredada de VPR-9713) no cambia — ahora
  aplica sobre el campo `ownerTemplateCode` de cada assignment, no sobre un string plano de lista.
- `namespace` no tiene restricción de unicidad — varios assignments pueden compartir el mismo
  namespace (ej. `metadata.amount` para distintos owners).
- Confirmado: es un cambio de forma sobre el nodo `transferInstructions` existente, no un nodo
  nuevo — mismo campo en `DistributionConfigPayload`, mismo endpoint
  `PUT /configs/{id}/transfer-instructions`.
