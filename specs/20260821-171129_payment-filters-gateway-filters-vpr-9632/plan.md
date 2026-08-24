**Created at**: 2026-08-21
**Status**: Approved
**Based on story**: @specs/20260821-171129_payment-filters-gateway-filters-vpr-9632/story.md

# Plan: Payment Filters — Gateway Filters

### Goal
Tipar la sección `gatewayFilters` de `DistributablePaymentsConfig` (hoy `JsonNode` placeholder,
VPR-9631) y extender el mismo `UpdateDistributablePaymentsUseCase` para que la persista, sin
duplicar el endpoint `PUT /configs/{id}/payment-filters`.

### Context
- `DistributablePaymentsConfig.java` (VPR-9631) — el campo `gatewayFilters` pasa de `JsonNode` a
  `GatewayFiltersConfig`.
- `UpdateDistributablePaymentsUseCase.java` (VPR-9631) — hoy preserva `gatewayFilters` como el
  `JsonNode` existente sin tocarlo; se extiende para construirlo a partir del request.
- Los nombres de gateway son `String` libre (lista fija hoy: `BANCOLOMBIA, EFECTY, NEQUI,
  DAVIPLATA, PSE, PayU, WOMPI, JP_MORGAN`), no un enum cerrado — a futuro se sincroniza con la API,
  fuera de alcance de esta historia.

### Public Contracts
- **Domain**: `GatewayFilterMode` (enum: `ALL`, `INCLUDE_ONLY`, `EXCLUDE`),
  `GatewayFiltersConfig(mode, gateways)`.
- **DTO**: `UpdateGatewayFiltersRequest(mode, gateways)`; `UpdatePaymentFiltersRequest` extendido
  con el campo `gatewayFilters`.
- **Endpoint**: sin cambios — `PUT /configs/{id}/payment-filters` (ya existe, VPR-9631).
- **Tests**: extender `UpdateDistributablePaymentsUseCaseTest` con los casos de Gateway Filters.

### Phases

#### Phase 1: Modelo tipado + use case
[Reemplaza el placeholder JsonNode y extiende el use case existente — no crea un endpoint nuevo.]
- [ ] `GatewayFilterMode`, `GatewayFiltersConfig`
- [ ] `DistributablePaymentsConfig.gatewayFilters` de `JsonNode` a `GatewayFiltersConfig`
- [ ] `UpdateGatewayFiltersRequest` (DTO) + `UpdatePaymentFiltersRequest.gatewayFilters`
- [ ] `UpdateDistributablePaymentsUseCase`: `mode` default `ALL`; si `ALL`, ignora `gateways`
  recibidos y persiste lista vacía; si `INCLUDE_ONLY`/`EXCLUDE`, requiere ≥1 gateway o rechaza

#### Phase 2: Tests
[Cubre defaults, las 2 validaciones nuevas y que no rompe los tests de Accounting Payments existentes.]
- [ ] `mode=INCLUDE_ONLY`/`EXCLUDE` con gateways → persiste tal cual
- [ ] `mode=ALL` con gateways en el request → persiste lista vacía (se ignoran)
- [ ] `mode=INCLUDE_ONLY`/`EXCLUDE` sin gateways → error
- [ ] `mode` no enviado → default `ALL`
- [ ] `execute_preservesRestOfPayload`/`execute_preservesExistingGatewayFilters` (ya existentes en
  `UpdateDistributablePaymentsUseCaseTest`) se actualizan o eliminan según corresponda, dado que
  `gatewayFilters` deja de ser un `JsonNode` opaco

### Next Step
Payment Filters queda completo (Accounting Payments + Gateway Filters). Siguiente etapa del wizard:
Conciliation Requirements (VPR-9633) y Date & Time Filters (VPR-9634), ambos visibles en el mismo
mockup (`docs/screen-payments-filters-2.png`) como cards separadas de esta etapa.
