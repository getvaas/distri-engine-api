# Original Request

Jira: VPR-9666 — "Ejecución: cálculo de netAmount / amountField con fallback" (Bloque 3).

Descripción: en `master-trust-servicer-api`, `DefaultDistributionParametersCalculator.getNetAmount()`
usa un fallback de 3 niveles: `pt.netAmount` si existe; si no, `totalPayment - feeAmount`; si no,
`totalPayment` crudo (falla si ni eso existe). Alimentado por Pool Strategy → Amount field
(VPR-9628, Done).

**Decisión a tomar** (la más concreta según el propio ticket): VPR-9628 no especifica qué hacer si
el `amountField` configurado viene `null` en una tape puntual — ¿cae a este mismo fallback de 3
niveles, o falla explícito la tape (como pide E9 para el guardrail de moneda)?

**Investigación antes de decidir:**
- "E9" es sobre otro bug (mezcla de monedas en `AssignmentsResolver.kt`, no sobre `amountField`) —
  el ticket lo cita solo como precedente de filosofía: fallar explícito en vez de calcular
  silenciosamente un número incorrecto.
- El fallback de 3 niveles real está roto en la práctica hoy: `fee_amount` es una columna real
  confirmada pero **siempre `null`** (bug documentado del extractor — el campo nunca se bindea).
  `total_payment` **no está confirmado como columna real** en ningún doc ni en este repo — solo
  aparece mencionado dentro de la descripción del fallback.

**Decisión del usuario**: no generalizar el fallback de 3 niveles (no puede funcionar de verdad hoy
con esos 2 problemas de datos). Si el `amountField` configurado (columna real y soportada, ej.
`net_amount`) viene `null` en una tape puntual, esa tape falla explícito.

Ref: `docs/proceso-distribucion-unificado.md`, Sección 1 paso 5b · Sección 2 punto 4.
