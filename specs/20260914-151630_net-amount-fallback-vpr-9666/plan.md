**Created at**: 2026-09-14
**Status**: Done
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
- [x] `NullAmountFieldValueException` (nueva) con el id de la tape y el `amountField` afectado en el mensaje.
- [x] `resolveAmount` lanza la excepción nueva si el valor resuelto es `null`, después de la validación de campo soportado.
- [x] Registrar en `GlobalExceptionHandler` (409).
- [x] `FetchEligiblePaymentTapesUseCaseTest`: nuevo test (`execute_amountFieldNullOnSpecificTape_throws`); tests existentes (incluido `execute_unsupportedAmountField_throws`) siguen pasando sin cambios de comportamiento.

### Next Step
Fase implementada y verificada con una corrida real de tests en verde (`./scripts/run-tests.sh`,
confirmado por el usuario en la rama feature/VPR-9667-ownerless-partition, que incluye este código).
