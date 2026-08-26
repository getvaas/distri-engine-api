**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-155413_ownership-source-and-cross-validation-vpr-9635-9636/story.md

# Plan: Ownership — Source y Cross Validation

### Goal
Tipar el placeholder `ownership` de `DistributionConfigPayload` (hoy `JsonNode`) con Source
(VPR-9635) y Cross Validation (VPR-9636), con un endpoint nuevo dedicado a esta etapa.

### Context
- `DistributionConfigPayload.ownership` pasa de `JsonNode` a `OwnershipConfig`.
- Mismo patrón general que las demás etapas: un use case nuevo, un endpoint nuevo
  (`PUT /configs/{id}/ownership`), preserva el resto del payload intacto.
- `field` es `String` libre — mismo patrón que `amountField`; su significado depende de
  `sourceType` (columna de contract_id vs. columna del owner en el tape).

### Public Contracts
- **Domain**: `OwnershipSourceType` (enum: `OWNERSHIP_API`, `PAYMENT_TAPE_FIELD`),
  `OwnershipSourceConfig(sourceType, field, defaultOwner)`,
  `OwnershipMismatchStrategy` (enum: `API_WINS`, `TAPE_WINS`, `BLOCK_PAYMENT`,
  `BLOCK_DISTRIBUTION`), `OwnershipCrossValidationConfig(enabled, mismatchStrategy)`,
  `OwnershipConfig(source, crossValidation)`.
- **DTO**: `UpdateOwnershipSourceRequest(sourceType, field, defaultOwner)`,
  `UpdateOwnershipCrossValidationRequest(enabled, mismatchStrategy)`,
  `UpdateOwnershipRequest(source, crossValidation)`.
- **Endpoint**: `PUT /configs/{id}/ownership` (nuevo).
- **Use case**: `UpdateOwnershipUseCase` (nuevo).
- **Tests**: `UpdateOwnershipUseCaseTest` (nuevo).

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de ownership.]
- [ ] `OwnershipSourceType`, `OwnershipSourceConfig`, `OwnershipMismatchStrategy`,
  `OwnershipCrossValidationConfig`, `OwnershipConfig`
- [ ] `DistributionConfigPayload.ownership` de `JsonNode` a `OwnershipConfig`
- [ ] `UpdateOwnershipSourceRequest`, `UpdateOwnershipCrossValidationRequest`,
  `UpdateOwnershipRequest` (DTOs)

#### Phase 2: Use case + validaciones
- [ ] `sourceType` y `field` requeridos si se envía `source`
- [ ] `defaultOwner` opcional, sin default forzado
- [ ] Si `crossValidation.enabled=false`: `mismatchStrategy` se ignora, persiste `null`
- [ ] Si `crossValidation.enabled=true`: `mismatchStrategy` requerido
- [ ] `source`/`crossValidation` no enviados → persisten `null` sin error (etapa opcional)

#### Phase 3: Endpoint
- [ ] `PUT /configs/{id}/ownership` en `DistributionConfigRouter`

#### Phase 4: Tests
- [ ] `sourceType=OWNERSHIP_API` con `field` → persiste tal cual
- [ ] `sourceType=PAYMENT_TAPE_FIELD` con `field` tipo `extra_data.aux_var_3` → persiste tal cual
- [ ] `source` sin `field` → error
- [ ] `defaultOwner` no enviado → `null`, sin error
- [ ] `crossValidation.enabled=true` con cada una de las 4 `mismatchStrategy` → persiste tal cual
- [ ] `crossValidation.enabled=true` sin `mismatchStrategy` → error
- [ ] `crossValidation.enabled=false` con `mismatchStrategy` enviado → se ignora, persiste `null`
- [ ] Preserva el resto del payload sin tocar

### Next Step
Riesgos documentados, no resueltos aquí: fallback si la Ownership API externa cae (timeout);
reuso de la partición ownerless para `BLOCK_PAYMENT` (decisión de ejecución); NPE de
`p.ownerName!!` en el motor real (bug de código, no de config); capa de normalización/alias de
owner (Finamco/Liquitech).
