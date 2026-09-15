**Created at**: 2026-09-07
**Status**: Done
**Based on story**: @story.md

# Plan: Gate de conciliación con tolerancia configurable

### Goal
Construir `ConciliationToleranceCheck` (4to readiness check, VPR-9663) que cuenta el % no
conciliado del pool candidato contra una tolerancia configurable en `ConciliationRequirementsConfig`
(VPR-9633), evaluando reglas contra columnas reales de `payment_tape`.

### Context
- `src/main/java/com/getvaas/distribution/engine/domain/model/ConciliationRequirementsConfig.java` — gana `tolerancePercentage`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/dto/UpdatePaymentFiltersRequest.java`, `application/usecase/PaymentFiltersConfigBuilder.java` — ganan/validan `conciliationTolerancePercentage`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/payments/entity/PaymentTapeEntity.java` — gana `paymentId`/`fundTransferId`.
- `src/main/java/com/getvaas/distribution/engine/domain/model/ReadinessCheckContext.java`, `application/usecase/RunReadinessChecksUseCase.java` — gana `conciliationRequirements`.
- `src/main/java/com/getvaas/distribution/engine/domain/model/enums/ReadinessCheckType.java` — gana `CONCILIATION_TOLERANCE`.
- `src/main/java/com/getvaas/distribution/engine/domain/service/readiness/` — nuevo check va acá, junto a `PaymentTapeLoadedCheck`/`NoDuplicateDistributionCheck`.

### Public Contracts
- **Services**: `ConciliationToleranceCheck.evaluate(context) -> ReadinessCheckResult` (type `CONCILIATION_TOLERANCE`)
- **Domain model**: `ConciliationRequirementsConfig(groups, tolerancePercentage: Integer)`
- **Excepciones**: `UnsupportedConciliationRuleException` (409)
- **Database**: sin migraciones — 2 columnas nuevas mapeadas sobre `payment_tape` ya existente (`payment_id`, `fund_transfer_id`).

### Phases

#### Phase 1: Config y contexto
- [x] `ConciliationRequirementsConfig` gana `tolerancePercentage` (Integer, 0-100, nullable).
- [x] `UpdatePaymentFiltersRequest`/`PaymentFiltersConfigBuilder` — validan rango, pasan `conciliationTolerancePercentage` a través.
- [x] `PaymentTapeEntity` gana `paymentId`/`fundTransferId` (String).
- [x] `ReadinessCheckContext` gana `conciliationRequirements`; `RunReadinessChecksUseCase` lo arma desde `config.config().paymentFilters()`.
- [x] `ReadinessCheckType.CONCILIATION_TOLERANCE` nuevo (no agregado a los defaults — opt-in explícito).
- [x] Actualizar todos los tests existentes que construyen `ReadinessCheckContext`/`UpdatePaymentFiltersRequest` por los campos nuevos.

#### Phase 2: El check
- [x] `ConciliationToleranceCheck implements ReadinessCheck`: auto-pasa si `pool.strategy() != PAYMENT_TAPE`, sin `conciliationRequirements`/`tolerancePercentage`, o sin grupos. Si aplica: trae el pool candidato (misma ventana que `PaymentTapeLoadedCheck`), evalúa cada tape contra los grupos (OR entre grupos, AND dentro de grupo), cuenta % no conciliado, compara contra la tolerancia.
- [x] Reglas soportadas: `PAYMENT_TAPE` vs `PAYMENTS` (`payment_id IS NOT NULL`), `PAYMENT_TAPE` vs `FUNDS_TRANSFER` (`fund_transfer_id IS NOT NULL`), sin `gateway` específico. Cualquier otra combinación → `UnsupportedConciliationRuleException`.
- [x] `ConciliationToleranceCheckTest`: auto-pass (estrategia distinta, sin tolerancia, sin grupos, pool vacío), over/under tolerancia, OR entre grupos, AND dentro de grupo, reglas no soportadas (tabla y gateway).

### Next Step
Las 2 fases están implementadas y verificadas con una corrida real de tests en verde
(`./scripts/run-tests.sh`, confirmado por el usuario). Ver `resume.md`.
