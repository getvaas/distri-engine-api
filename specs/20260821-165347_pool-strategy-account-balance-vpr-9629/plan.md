**Created at**: 2026-08-21
**Status**: Approved
**Based on story**: @specs/20260821-165347_pool-strategy-account-balance-vpr-9629/story.md

# Plan: Pool Strategy — Account Balance

### Goal
Tipar la sección `accountBalance` de `PoolConfig` (hoy `JsonNode`) y extender el mismo
`UpdatePoolConfigUseCase` de VPR-9628 para que soporte esta estrategia, sin duplicar el endpoint.

### Context
- `distri-engine-api/.../PoolConfig.java` (VPR-9628) — el campo `accountBalance` pasa de `JsonNode` a
  `AccountBalancePoolConfig`.
- `distri-engine-api/.../UpdatePoolConfigUseCase.java` (VPR-9628) — se extiende con la rama
  `ACCOUNT_BALANCE`; el endpoint `PUT /configs/{id}/pool` sigue siendo el único punto de entrada para
  toda la etapa Pool Strategy, sin importar la estrategia elegida.
- Conversación previa sobre `account_balance` (schema real: `current_balance`, `projected_balance`) —
  fuente de la decisión de solo 2 `balanceType` reales.

### Public Contracts
- **Domain**: `PoolBalanceType` (enum: `CURRENT_BALANCE`, `USABLE_BALANCE`),
  `AccountBalanceSource(accountId, balanceType, description)`,
  `AccountBalancePoolConfig(accounts)`.
- **DTO**: `AccountBalanceSourceRequest`, `UpdatePoolConfigRequest` extendido con `accounts`.
- **Endpoint**: sin cambios — `PUT /configs/{id}/pool` (ya existe, VPR-9628).
- **Tests**: extender `UpdatePoolConfigUseCaseTest` con los casos de `ACCOUNT_BALANCE`.

### Phases

#### Phase 1: Modelo tipado
[Reemplaza el placeholder JsonNode de accountBalance.]
- [ ] `PoolBalanceType`, `AccountBalanceSource`, `AccountBalancePoolConfig`
- [ ] `PoolConfig.accountBalance` de `JsonNode` a `AccountBalancePoolConfig`
- [ ] `AccountBalanceSourceRequest` (DTO) + `UpdatePoolConfigRequest.accounts`

#### Phase 2: Use case + validaciones
[Extiende UpdatePoolConfigUseCase — no crea un endpoint nuevo.]
- [ ] Rama `ACCOUNT_BALANCE`: default `USABLE_BALANCE` por cuenta si no se especifica
- [ ] Validación: al menos 1 cuenta
- [ ] Validación: sin `accountId` duplicado

#### Phase 3: Tests
[Cubre los defaults y las 2 validaciones nuevas, sin romper los tests de PAYMENT_TAPE ya existentes.]
- [ ] Cuentas con y sin `balanceType` explícito
- [ ] Sin cuentas → error
- [ ] `accountId` duplicado → error
- [ ] Cambiar de `PAYMENT_TAPE` a `ACCOUNT_BALANCE` no deja restos de la config anterior

### Next Step
Completado — `DATA_SOURCE_AGGREGATION` (VPR-9630) queda igual de placeholder, fuera de alcance hasta que
un deal la pida (confirmado en conversación previa).
