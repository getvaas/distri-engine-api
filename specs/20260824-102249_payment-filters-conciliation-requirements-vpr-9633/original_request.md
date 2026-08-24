# Original Request

**Source**: Jira ticket [VPR-9633](https://pmvaas1.atlassian.net/browse/VPR-9633) — "Payment Filters: conciliation Requirements"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-24, post-refinamiento)

Reglas que indican qué conciliaciones son requeridas para poder distribuir — pares de tablas
(`payment_tape`, `payments`, `funds_transfer`, `disbursements`, `borrower_core`) + gateway
(`All gateways` o específico). Mismo patrón de builder AND/OR que Accounting Payments (VPR-9631).

**Resuelto:**
1. Requisito booleano, no gate de tolerancia — el % de tolerancia (E8) queda pendiente en otra
   etapa (candidato: Readiness Checks).
2. Cualquier combinación de tablas es válida salvo `tableA == tableB`.
3. `All gateways` es un campo independiente por regla, no comparte estado con Gateway Filters
   (VPR-9632).
4. Gap confirmado contra el motor real (`ConciliationRepositoryHelper`/`ConciliationType` en
   `master-trust-servicer-api`, verificado leyendo el código fuente en esta sesión): el enum real
   solo tiene `PAYMENTS_VS_PAYMENT_TAPE`, `PAYMENTS_VS_BORROWERS_CORE`, `PAYMENTS_VS_BANK` — Funds
   Transfer y Disbursements quedan implícitos dentro de `PAYMENTS_VS_BANK`, sin representación
   propia. Decisión: el wizard igual ofrece las 5 opciones; el mapeo real queda como riesgo
   abierto para Readiness Checks, no bloquea esta historia.

Ref: `docs/screen-payments-filters-2.png` · `docs/distribution-engine-onboarding.html` STEP 2 ·
`docs/epica-distri-engine.md` E8.

## User additions (this session)

- Usuario confirmó la forma final del modelo en sus propias palabras: "una lista donde source y
  table estén en true, eso se valide y listo... hay que tener en cuenta la permutabilidad... pero
  eso no sería un problema" — es decir, una lista de pares requeridos, sin necesidad de normalizar
  o deduplicar por orden de los elementos del par.
- Nuevo campo `conciliationRequirements` en `DistributablePaymentsConfig` (junto a
  `accountingPayments` y `gatewayFilters`), mismo endpoint `PUT /configs/{id}/payment-filters`.
