**Created at**: 2026-09-08
**Status**: Done
**Based on story**: @story.md

# Plan: Aplicar Payment Filters sobre el pool candidato

### Goal
Construir `ApplyPaymentFiltersUseCase` que aplica Accounting Payments, Gateway Filters y Date &
Time Filters en secuencia sobre los payment tapes candidatos, e integrarlo en
`FetchEligiblePaymentTapesUseCase` para que todo el pipeline se beneficie sin cambios adicionales.

### Context
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/payments/entity/PaymentTapeEntity.java` — gana `gatewayCode`.
- `src/main/java/com/getvaas/distribution/engine/domain/model/DateTimeFilterRule.java` — gana `cutoffTime`, `businessDays`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/dto/DateTimeFilterRuleRequest.java`, `application/usecase/PaymentFiltersConfigBuilder.java` — ganan/parsean los campos nuevos.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/FetchEligiblePaymentTapesUseCase.java` — invoca el filtro nuevo después de la query, antes de resolver `amount`.

### Public Contracts
- **Services**: `ApplyPaymentFiltersUseCase.execute(PaymentFiltersConfig, List<PaymentTapeEntity>, LocalDate, String) -> List<PaymentTapeEntity>`
- **Domain model**: `DateTimeFilterRule(gateway, ruleType, operator, value, maxDays, cutoffTime: LocalTime, businessDays: Boolean)`
- **Excepciones**: `UnsupportedPaymentFilterFieldException` (409)
- **Database**: sin migraciones — 1 columna nueva mapeada sobre `payment_tape` ya existente (`gateway_code`).

### Phases

#### Phase 1: Campos nuevos
- [x] `PaymentTapeEntity` gana `gatewayCode` (String).
- [x] `DateTimeFilterRule`/`DateTimeFilterRuleRequest` ganan `cutoffTime`/`businessDays`; `PaymentFiltersConfigBuilder` parsea `cutoffTime` (HH:mm) con validación explícita de formato.
- [x] Actualizar todos los tests existentes que construyen `DateTimeFilterRuleRequest`/`DateTimeFilterRule` por los campos nuevos.

#### Phase 2: El use case de filtrado
- [x] `ApplyPaymentFiltersUseCase`: Accounting Payments (excluye si matchea algún grupo y `distributeAccountingPayments=false`), Gateway Filters (`INCLUDE_ONLY`/`EXCLUDE` contra `gateway_code`), Date & Time Filters (`DISTRIBUTE_BY_DATE`/`DISTRIBUTE_BY_DATE_TIME` con `cutoffTime`, `DAYS_BACK_LIMIT` con `businessDays` reusando `WorkingDaysCalculator`), todos respetando el `gateway` scoping por regla.
- [x] Condiciones de Accounting Payments solo evalúan columnas ya mapeadas; campo no soportado o `GT`/`LT` sobre campo no numérico → `UnsupportedPaymentFilterFieldException`.
- [x] `FetchEligiblePaymentTapesUseCase` invoca el filtro después de la query de ventana.
- [x] `ApplyPaymentFiltersUseCaseTest`: 15 tests (cada filtro solo, combinados en secuencia, gateway-scoping, campos no soportados).

### Next Step
Las 2 fases están implementadas y verificadas con una corrida real de tests en verde
(`./scripts/run-tests.sh`, confirmado por el usuario). Ver `resume.md`.
