**Created at**: 2026-09-14
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Fallar explícito si amountField viene null en una tape puntual

### Description
Hoy, si la columna configurada como `amountField` (`net_amount`/`gross_amount`) existe en la tabla
pero viene `null` en una tape en particular, `FetchEligiblePaymentTapesUseCase` deja pasar ese
`null` sin ningún control — el pool terminaría con un fondo sin monto. Esto cierra esa brecha: la
tape falla explícito con un mensaje claro, en vez de calcular en base a un dato ausente o dejar
pasar un `null` silencioso.

### Acceptance Criteria
- [x] **Given** un `amountField` configurado y soportado (`net_amount`/`gross_amount`), **When** esa columna viene `null` para una tape puntual, **Then** se lanza `NullAmountFieldValueException` con el id de la tape y el campo afectado, en vez de continuar con un monto `null`.
- [x] **Given** el mismo `amountField` con valor no nulo, **When** se resuelve, **Then** el comportamiento existente no cambia.
- [x] **Given** un `amountField` no soportado (ni `net_amount` ni `gross_amount`), **When** se resuelve, **Then** sigue lanzando `UnsupportedPoolAmountFieldException` como antes (sin conflicto con la excepción nueva).

### Additional Context
- Jira: VPR-9666. Referencia: `docs/proceso-distribucion-unificado.md` Sección 1 paso 5b, Sección 2 punto 4.
- Decisión explícita de no generalizar el fallback de 3 niveles del motor real (`netAmount` → `totalPayment - feeAmount` → `totalPayment`): `fee_amount` está confirmado siempre `null` hoy (bug del extractor) y `total_payment` no es una columna verificada en este repo — implementar ese fallback hubiera significado construir sobre datos rotos/no confirmados.
- "E9" (guardrail de moneda) se cita en el ticket solo como precedente de principio (fallar explícito en vez de calcular silenciosamente mal), no como mecanismo a portar — es un bug distinto (mezcla de monedas en `AssignmentsResolver.kt`), no relacionado con `amountField`.
