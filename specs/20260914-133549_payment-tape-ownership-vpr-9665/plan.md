**Created at**: 2026-09-14
**Status**: Done
**Based on story**: @story.md

# Plan: Resolver el owner de cada payment tape (PAYMENT_TAPE_FIELD)

### Goal
Construir `ResolveOwnershipUseCase` para el resolver `PAYMENT_TAPE_FIELD` sobre `owner_name`, con
fallback correcto a `defaultOwner`/`"UNDEFINED"`, e integrarlo en el pipeline existente sin romper
ninguna pieza ya construida.

### Context
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/payments/entity/PaymentTapeEntity.java` — gana `ownerName`.
- `src/main/java/com/getvaas/distribution/engine/domain/model/EligiblePaymentTape.java`, `PoolFund.java` — ganan `owner`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/FetchEligiblePaymentTapesUseCase.java` — invoca el resolver junto con `amount`.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/PaymentTapePoolStrategyResolver.java` — propaga `owner` a `PoolFund`.

### Public Contracts
- **Services**: `ResolveOwnershipUseCase.execute(PaymentTapeEntity, OwnershipConfig) -> String` (nunca null, `ResolveOwnershipUseCase.UNDEFINED_OWNER = "UNDEFINED"`)
- **Domain model**: `EligiblePaymentTape(id, companyId, paymentDate, amount, owner)`, `PoolFund(sourceId, amount, owner)`
- **Excepciones**: `UnsupportedOwnershipFieldException` (409), `UnsupportedOwnershipSourceException` (409)
- **Database**: sin migraciones — 1 columna nueva mapeada sobre `payment_tape` ya existente (`owner_name`).

### Phases

#### Phase 1: Campo nuevo y el resolver
- [x] `PaymentTapeEntity` gana `ownerName` (String).
- [x] `ResolveOwnershipUseCase`: `PAYMENT_TAPE_FIELD`+`owner_name` → valor de la columna, con fallback a `defaultOwner` y luego a `"UNDEFINED"`. `field` distinto de `owner_name`, `sourceType=OWNERSHIP_API`, o `crossValidation.enabled=true` → excepción explícita.
- [x] `ResolveOwnershipUseCaseTest`: 9 tests (todos los fallbacks, ambas excepciones, cross-validation deshabilitada no bloquea).

#### Phase 2: Wiring en el pipeline
- [x] `EligiblePaymentTape`/`PoolFund` ganan `owner`; `FetchEligiblePaymentTapesUseCase` resuelve el owner junto con el monto; `PaymentTapePoolStrategyResolver` lo propaga.
- [x] Actualizar todos los tests existentes que construían `EligiblePaymentTape`/`PoolFund` por el campo nuevo; agregar 2 tests de wiring end-to-end en `FetchEligiblePaymentTapesUseCaseTest`.

### Next Step
Las 2 fases están implementadas y verificadas con una corrida real de tests en verde
(`./scripts/run-tests.sh`, confirmado por el usuario). Ver `resume.md`.
