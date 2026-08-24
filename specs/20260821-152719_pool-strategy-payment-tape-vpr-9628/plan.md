**Created at**: 2026-08-21
**Status**: Done
**Based on story**: @specs/20260821-152719_pool-strategy-payment-tape-vpr-9628/story.md

# Plan: Pool Strategy — Payment Tape

### Goal
Tipar la sección `pool` del `DistributionConfigPayload` para el caso Payment Tape, con un endpoint
dedicado que la actualiza sin afectar Deal Info ni el resto de las secciones.

### Context
- `distri-engine-api/.../DistributionConfigPayload.java` — el campo `pool` pasa de `JsonNode` a
  `PoolConfig`.
- `distri-engine-api/.../UpdateDistributionConfigUseCase.java` — mismo patrón de "mutar la entidad
  gestionada + reserializar" a reusar.
- `docs/distribution-engine-onboarding.html` STEP 1 — mockup real de los campos (`Amount field`, `Days
  back`).

### Public Contracts
- **Domain**: `PoolStrategyType` (enum), `PoolConfig(strategy, paymentTape, accountBalance,
  dataSourceAggregation)`, `PaymentTapePoolConfig(amountField, daysBack)`.
- **Endpoints**: `PUT /configs/{id}/pool`.
- **Tests**: `UpdatePoolConfigUseCaseTest` (defaults, valores custom, otras strategies, validación, 404,
  no pisa Deal Info).

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de `pool` por la estructura real de Payment Tape.]
- [x] `PoolStrategyType`, `PaymentTapePoolConfig`, `PoolConfig`
- [x] `DistributionConfigPayload.pool` de `JsonNode` a `PoolConfig`
- [x] `DistributionConfigResponse` expone `pool`

#### Phase 2: Use case + endpoint + validación
[Expone la configuración con sus defaults y su única regla de validación.]
- [x] `UpdatePoolConfigUseCase` (defaults, validación de `daysBack` negativo)
- [x] `InvalidDistributionConfigException` + handler 400
- [x] Endpoint `PUT /configs/{id}/pool`

#### Phase 3: Tests
[Cubre defaults, valores custom, las otras 2 strategies sin config propia, y los casos de error.]
- [x] 6 casos: defaults, custom, otra strategy, negativo→error, 404, preserva Deal Info

### Next Step
Completado — siguiente etapa candidata: Payment Filters (VPR-9631-9634) o el lado de ejecución (VPR-9660).
