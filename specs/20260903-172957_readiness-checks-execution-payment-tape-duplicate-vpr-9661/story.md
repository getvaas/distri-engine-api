**Created at**: 2026-09-03
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: —

# Story: Implementar los checks de Readiness pendientes (payment tape cargado, sin duplicados)

### Description
El motor de Readiness Checks (VPR-9661) ya tiene su estructura base y un check real funcionando
(`BusinessDayCheck`) — construido en una iteración anterior. Faltan los otros dos checks que ya
están modelados en la configuración (`ReadinessCheckType.PAYMENT_TAPE_LOADED` y
`NO_DUPLICATE_DISTRIBUTION`) pero que el runner reporta como `NOT_IMPLEMENTED`. Sin estos dos, un
deal que los habilite en su config no obtiene ninguna validación real antes de distribuir — el
motor "deja pasar" precondiciones que el negocio espera que se chequeen de verdad.

### Acceptance Criteria
- [ ] **Given** una `DistributionConfig` con `pool.strategy = PAYMENT_TAPE` y al menos un payment tape sin distribuir dentro de la ventana configurada, **When** corre `PaymentTapeLoadedCheck`, **Then** el resultado es `PASSED`.
- [ ] **Given** la misma config pero sin ningún payment tape candidato en la ventana, **When** corre el check, **Then** el resultado es `FAILED`.
- [ ] **Given** una `DistributionConfig` con `pool.strategy = ACCOUNT_BALANCE` o `DATA_SOURCE_AGGREGATION`, **When** corre `PaymentTapeLoadedCheck`, **Then** el resultado es `PASSED` automáticamente, sin consultar payment tapes.
- [ ] **Given** el check `NO_DUPLICATE_DISTRIBUTION` habilitado para un deal, **When** ya existe una fila en `master_trust_servicer.distribution` con ese `masterTrustId`, `active=1` y `distribution_date` (por fecha calendario) igual a la fecha evaluada, **Then** el resultado es `FAILED`.
- [ ] **Given** el mismo escenario pero sin ninguna fila así (o con `active` nulo/0), **When** corre el check, **Then** el resultado es `PASSED`.
- [ ] **Given** el schema de `readinessChecks`, **When** se persiste un `ReadinessCheckSetting` de tipo `BUSINESS_DAY`, **Then** admite un campo nuevo `forceRunOnNonBusinessDay` (booleano, opcional) que se guarda y se lee correctamente, sin que `BusinessDayCheck` cambie su comportamiento actual (sigue bloqueando en fin de semana sin importar este campo).

### Additional Context
- Jira: VPR-9661 — continuación de `specs/20260821-154114_readiness-checks-engine-business-day-vpr-9661` (Done: motor + `BusinessDayCheck`).
- `PaymentTapeLoadedCheck` reusa el criterio de "candidato" de `FetchCandidatePaymentTapesUseCase` (VPR-9662) — misma ventana de días hábiles hacia atrás desde `pool.paymentTape().daysBack()`.
- La tabla `master_trust_servicer.distribution` es propiedad de `master-trust-servicer-api` — se mapea de solo lectura (mismo criterio que `payment_tape` en `payments_db`: nunca migrarla ni alterarla desde este repo).
- `forceRunOnNonBusinessDay` es un campo reservado — su comportamiento real (forzar distribución en día no hábil) queda fuera de alcance, para un ticket futuro.
- Notificaciones al fallar un check y calendario de feriados en `BusinessDayCheck` siguen fuera de alcance (decisiones abiertas del ticket original, sin resolver).
