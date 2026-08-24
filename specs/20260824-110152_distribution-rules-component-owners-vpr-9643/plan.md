**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-110152_distribution-rules-component-owners-vpr-9643/story.md

# Plan: Distribution Rules — Owner por componente de la cuota

### Goal
Tipar el placeholder `rules` de `DistributionConfigPayload` (hoy `JsonNode`) con el modelo mínimo
de owner-por-componente, y exponer un endpoint nuevo dedicado a esta etapa del wizard.

### Context
- `DistributionConfigPayload.java` — `rules` pasa de `JsonNode` a `DistributionRulesConfig`.
- Nueva etapa del wizard (Etapa 5), no comparte endpoint con Payment Filters — necesita su propio
  use case y su propio `PUT`, siguiendo el mismo patrón general (leer config existente, tipar una
  sección, preservar el resto).
- `owner` es `String` libre — mismo patrón abierto que `gateway` en Gateway Filters/Conciliation
  Requirements. Resolverlo contra partes reales queda para Ownership (VPR-9635/9636).

### Public Contracts
- **Domain**: `PaymentComponent` (enum: `PRINCIPAL`, `INTEREST`, `LATE_FEE`, `GUARANTEE`),
  `ComponentOwnerRule(component, owner, description)`,
  `DistributionRulesConfig(componentOwners)`.
- **DTO**: `ComponentOwnerRuleRequest`, `UpdateDistributionRulesRequest(componentOwners)`.
- **Endpoint**: `PUT /configs/{id}/distribution-rules` (nuevo).
- **Use case**: `UpdateDistributionRulesUseCase` (nuevo).
- **Tests**: `UpdateDistributionRulesUseCaseTest` (nuevo).

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de rules.]
- [ ] `PaymentComponent`, `ComponentOwnerRule`, `DistributionRulesConfig`
- [ ] `DistributionConfigPayload.rules` de `JsonNode` a `DistributionRulesConfig`
- [ ] `ComponentOwnerRuleRequest`, `UpdateDistributionRulesRequest` (DTOs)

#### Phase 2: Use case + validaciones
[Nuevo use case, nuevo endpoint.]
- [ ] Reglas no enviadas/vacías → persiste lista vacía sin error
- [ ] `component` y `owner` requeridos por regla
- [ ] Sin `component` repetido en la lista
- [ ] Preserva el resto del payload sin tocar

#### Phase 3: Endpoint
- [ ] `PUT /configs/{id}/distribution-rules` en `DistributionConfigRouter`

#### Phase 4: Tests
- [ ] Reglas válidas para los 4 componentes → persiste tal cual
- [ ] Regla sin `component` → error
- [ ] Regla sin `owner` → error
- [ ] `component` repetido → error
- [ ] Lista no enviada/vacía → persiste lista vacía, sin error
- [ ] Preserva el resto del payload (`pool`, `distributablePayments`, etc.)

### Next Step
Fees/deducciones, multi-moneda por regla, remanente/cascada e impuestos/seguros quedan
documentados como pendientes — no se resuelven en esta historia (decisión de scope explícita,
ver `story.md`).
