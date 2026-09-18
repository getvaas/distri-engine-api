# Original Request

Jira: VPR-9664 — "Ejecución: aplicar Payment Filters sobre el pool candidato".

Descripción: en `master-trust-servicer-api`, `DistributionFiltersResolver.resolve(date, paymentTapes,
config)` aplica Accounting Payments (VPR-9631), Gateway Filters (VPR-9632) y Date & Time Filters
(VPR-9634) sobre los payment tapes que sobrevivieron el gate de conciliación. La config real es una
lista plana `distributablePayments.paymentFilters[]` con un `type` enum (`DistributionFilterType`).

**Decisión a tomar** (resuelta con el usuario): confirmar si nuestras 3 sub-configs tipadas
(`AccountingPaymentsConfig`, `GatewayFiltersConfig`, `DateTimeFiltersConfig`) mapean 1:1 contra los
valores reales de `DistributionFilterType` — el usuario confirmó que sí, son exactamente esos 3
valores, sin necesidad de restructurar el modelo.

Decisiones adicionales tomadas antes de implementar:
1. `gateway_code` es la columna real en `payment_tape` para el gateway (confirmado por el usuario).
2. `cutoffTime` (HH:mm) y `businessDays` (booleano) son campos reales de Date & Time Filters
   (VPR-9634) que faltaban modelar — se agregan a `DateTimeFilterRule`.
3. Las condiciones de Accounting Payments (`field` arbitrario) siguen el criterio incremental ya
   usado para `amountField` (VPR-9628): solo columnas ya mapeadas en `PaymentTapeEntity`; cualquier
   otra falla explícito.

Ref: `docs/proceso-distribucion-unificado.md`, Sección 1 paso 4c.
