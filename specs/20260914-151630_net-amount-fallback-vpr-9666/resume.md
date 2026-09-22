**Created at**: 2026-09-15
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Fallar explícito si amountField viene null en una tape puntual

### Executive Summary
Si la columna de monto configurada (`amountField`) existe pero viene vacía en una tape puntual, el motor ahora lo detecta y falla esa tape con un mensaje claro, en vez de dejar pasar un fondo sin monto al pool.

### Technical Summary
- `NullAmountFieldValueException` (nueva): `resolveAmount` la lanza cuando el valor resuelto es `null`, incluyendo el id de la tape y el campo afectado en el mensaje.
- Se descartó generalizar el fallback de 3 niveles del motor real (`netAmount` → `totalPayment - feeAmount` → `totalPayment`): `fee_amount` está confirmado siempre `null` hoy (bug del extractor) y `total_payment` no es una columna verificada en este repo — el fallback no podría funcionar de verdad.
- "E9" (guardrail de moneda) se usó solo como precedente de principio, no como mecanismo a portar.
- Registrada en `GlobalExceptionHandler` (409).

### Phases Completed
- [x] **Phase 1**: Validación explícita — `NullAmountFieldValueException`, wiring en `resolveAmount`, tests.
