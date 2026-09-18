**Created at**: 2026-09-08
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Aplicar Payment Filters sobre el pool candidato

### Executive Summary
Los Payment Filters (Accounting Payments, Gateway Filters, Date & Time Filters) que un deal podía configurar hasta ahora sin que tuvieran ningún efecto real, ahora se aplican de verdad sobre el pool candidato antes de resolverlo — completando el "Bloque 2" del pipeline real (ventana, gate de conciliación, y ahora estos 3 filtros).

### Technical Summary
- `ApplyPaymentFiltersUseCase` (nuevo): aplica en secuencia Accounting Payments → Gateway Filters → Date & Time Filters; un tape debe sobrevivir los 3 para seguir en el pool.
- Accounting Payments excluye tapes que matchean algún grupo de condiciones configurado (OR de grupos, AND dentro de cada grupo) solo cuando `distributeAccountingPayments=false`; las condiciones solo evalúan columnas ya mapeadas en `PaymentTapeEntity` — un campo no soportado, o `GT`/`LT` sobre un campo no numérico, falla explícito (`UnsupportedPaymentFilterFieldException`).
- Gateway Filters (`INCLUDE_ONLY`/`EXCLUDE`) y el `gateway` scoping de Date & Time Filters se apoyan en la nueva columna `gateway_code` de `payment_tape`.
- `DateTimeFilterRule` gana `cutoffTime` (afina `DISTRIBUTE_BY_DATE_TIME` con una hora límite) y `businessDays` (si `DAYS_BACK_LIMIT` cuenta días hábiles vía `WorkingDaysCalculator`, o corridos).
- Se integró en `FetchEligiblePaymentTapesUseCase` sin cambiar su firma pública — todo el pipeline que ya lo consume (`PaymentTapePoolStrategyResolver` → `ResolveEligibleFundsUseCase` → `RunDistributionUseCase`) se beneficia automáticamente.
- Confirmado con el usuario: las 3 sub-configs ya existentes mapean 1:1 contra el enum real `DistributionFilterType` de producción — no hizo falta restructurar el modelo de config.

### Phases Completed
- [x] **Phase 1**: Campos nuevos — `gatewayCode` en `PaymentTapeEntity`, `cutoffTime`/`businessDays` en `DateTimeFilterRule`.
- [x] **Phase 2**: `ApplyPaymentFiltersUseCase` — implementado, integrado, y testeado (15 tests).
