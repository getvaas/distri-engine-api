# Original Request

**Source**: Jira ticket [VPR-9634](https://pmvaas1.atlassian.net/browse/VPR-9634) — "Payment Filters: Date & Time filters"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-24, post-refinamiento)

Rango de fechas a contemplar para la distribución, configurable por gateway:
`[gateway o All gateways] [Distribute by date | Distribute by date & time | Days back limit]
[Is before/Is after | Max days] [valor]`. Ejemplos: `All gateways, Distribute by date, Is before,
today` · `EFECTY, Days back limit, Max days, 3`.

**Resuelto:**
1. El "Days back" global de Pool Strategy (VPR-9628, default 90) es el mismo mecanismo que "Days
   back limit" aquí — esta etapa permite una excepción más fina por gateway sobre el default
   global (caso JTP: EFECTY bisemanal, PSE +1 día).
2. `today` es literalmente el día de hoy.
3. `Distribute by date` vs `Distribute by date & time` es solo diferencia de granularidad, mismo
   mecanismo de comparación.
4. El valor acepta `today` o fecha ISO absoluta, sin expresiones relativas por ahora.
5. Confirmado contra mockup + HTML fuente: esta card no tiene AND/OR, es lista plana
   (`+ Add filter rule` únicamente).

Ref: `docs/screen-payments-filters-2.png` · `docs/distribution-engine-onboarding.html` STEP 2 ·
`conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md` (JTP).

## User additions (this session)

- El usuario pidió reverificar contra el mockup si esta card tenía AND/OR como las otras dos —
  se confirmó con el HTML fuente (`docs/distribution-engine-onboarding.html` líneas 231-234) que
  no los tiene, solo `+ Add filter rule`. El malentendido inicial era sobre la frase "no muestra
  AND/OR", no sobre el modelo en sí — el usuario se refería a que la card en general existe en el
  mockup, no a los botones específicos.
- Nuevo campo `dateTimeFilters` en `DistributablePaymentsConfig` (junto a `accountingPayments`,
  `gatewayFilters`, `conciliationRequirements`), mismo endpoint `PUT /configs/{id}/payment-filters`.
