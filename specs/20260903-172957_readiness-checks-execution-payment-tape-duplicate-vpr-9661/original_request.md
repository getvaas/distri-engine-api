Jira: VPR-9661 — https://pmvaas1.atlassian.net/browse/VPR-9661
"Ejecución: motor de Readiness Checks (correr las precondiciones configuradas)"

Continuación del trabajo ya hecho en `specs/20260821-154114_readiness-checks-engine-business-day-vpr-9661`
(motor + `BusinessDayCheck`, Done). Esta iteración construye los 2 checks que faltaban:
`PAYMENT_TAPE_LOADED` y `NO_DUPLICATE_DISTRIBUTION`.

## Respuestas de clarificación (varias rondas)

**PaymentTapeLoadedCheck:**
- Deriva de `pool.strategy` (`PoolConfig`). Si `PAYMENT_TAPE`: calcula candidatos igual que
  `FetchCandidatePaymentTapesUseCase` (VPR-9662) — `PASSED` si encuentra al menos uno sin distribuir
  en la ventana configurada, `FAILED` si no encuentra ninguno. Si `ACCOUNT_BALANCE` o
  `DATA_SOURCE_AGGREGATION`: el deal no depende de payment tape → `PASSED` automático.

**NoDuplicateDistributionCheck:**
- Dos pasos: (1) el check está habilitado en `readinessChecks` (ya funciona así — presencia en
  `checks[]`), (2) si está habilitado, consultar la tabla `master_trust_servicer`.`distribution`
  (existente, propiedad de `master-trust-servicer-api`, mapeada de solo lectura vía el datasource
  `master_trust_servicer` que ya conectamos en VPR-9745).
- Condición de "ya distribuido ese día": existe una fila con `master_trust_servicer_id =
  config.masterTrustId()`, `active = 1` (tratar `NULL` como no-activa), y `distribution_date`
  (comparado por fecha calendario, no por timestamp exacto — la columna trae hora fija tipo
  13:00:00) igual a la fecha evaluada. Si existe → `FAILED` (duplicada). Si no → `PASSED`.
- Confirmado con datos reales de ejemplo de la tabla (compartidos por el usuario).
