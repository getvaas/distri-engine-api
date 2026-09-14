**Created at**: 2026-09-14
**Status**: Draft
**Based on story**: @story.md

# Plan: Fallar explícito si amountField viene null en una tape puntual

### Goal
Que `FetchEligiblePaymentTapesUseCase.resolveAmount` rechace explícito (`NullAmountFieldValueException`)
cualquier tape cuyo `amountField` configurado resuelva a `null`, en vez de dejarlo pasar en silencio.

### Context
- `src/main/java/com/getvaas/distribution/engine/application/usecase/FetchEligiblePaymentTapesUseCase.java` — `resolveAmount` gana la validación de `null`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/GlobalExceptionHandler.java` — mapea la excepción nueva.

### Public Contracts
- **Excepciones**: `NullAmountFieldValueException` (409)

### Phases

#### Phase 1: Validación explícita
- [ ] `NullAmountFieldValueException` (nueva) con el id de la tape y el `amountField` afectado en el mensaje. — implementado, compila limpio, pendiente de confirmar con tests reales.
- [ ] `resolveAmount` lanza la excepción nueva si el valor resuelto es `null`, después de la validación de campo soportado. — implementado, pendiente de confirmar.
- [ ] Registrar en `GlobalExceptionHandler` (409). — implementado, pendiente de confirmar.
- [ ] `FetchEligiblePaymentTapesUseCaseTest`: nuevo test (`execute_amountFieldNullOnSpecificTape_throws`); confirmar que los tests existentes (incluido `execute_unsupportedAmountField_throws`) siguen pasando sin cambios de comportamiento. — escrito, pendiente de correr.

### Next Step
Código implementado y compilando limpio. Esperando confirmación de una corrida real de tests
(`./scripts/run-tests.sh`) antes de tildar los checkboxes y marcar Done.
