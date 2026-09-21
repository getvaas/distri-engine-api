**Created at**: 2026-09-14
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Resolver el owner de cada payment tape (PAYMENT_TAPE_FIELD)

### Executive Summary
Cada fondo del pool candidato ahora trae resuelto el owner (la company que lo va a recibir), leído de la columna real `owner_name` de `payment_tape`, con un fallback correcto que nunca deja el valor en `null` — corrigiendo un bug real y documentado del motor de producción actual, donde un owner nulo mata toda la corrida en vez de tratarse como un caso manejable.

### Technical Summary
- `ResolveOwnershipUseCase` (nuevo): para `OwnershipSourceType.PAYMENT_TAPE_FIELD` sobre la columna `owner_name`, resuelve el valor real; si viene vacío, cae a `defaultOwner`; si tampoco hay, cae a `"UNDEFINED"` — nunca `null`.
- `OwnershipSourceType.OWNERSHIP_API` y `OwnershipCrossValidationConfig.enabled=true` fallan explícito (`UnsupportedOwnershipSourceException`) — decisión tomada tras confirmar que la Ownership API real requiere 2 saltos externos encadenados (`OwnerAtomHttpClient` para contract_id→owner vía Atom, `CompanyClient` para owner→company) que no existen en este repo. Quedan para un ticket futuro dedicado al Atom tracker.
- `field` distinto de `owner_name` falla explícito (`UnsupportedOwnershipFieldException`) — mismo criterio incremental que `amountField`/los campos de Payment Filters.
- `PaymentTapeEntity` gana `ownerName`; `EligiblePaymentTape`/`PoolFund` ganan `owner`, resuelto en `FetchEligiblePaymentTapesUseCase` junto con `amount` y propagado por `PaymentTapePoolStrategyResolver` — todo el pipeline (`ResolveEligibleFundsUseCase` → `RunDistributionUseCase`) se beneficia sin cambios adicionales.
- Explícitamente fuera de alcance: los 3 resolvers adicionales de VPR-9635 (Somos, Rapicredit, Finamco), y la partición ownerless-vs-distribuibles en sí (paso 6 del pipeline real) — este ticket solo resuelve el owner, no decide qué hacer con los que quedan "UNDEFINED".

### Phases Completed
- [x] **Phase 1**: `ownerName` en `PaymentTapeEntity` + `ResolveOwnershipUseCase` — implementado y testeado (9 tests).
- [x] **Phase 2**: Wiring en `EligiblePaymentTape`/`PoolFund`/`FetchEligiblePaymentTapesUseCase`/`PaymentTapePoolStrategyResolver` — integrado sin romper el pipeline existente.
