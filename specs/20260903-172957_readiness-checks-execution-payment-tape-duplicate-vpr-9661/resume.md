**Created at**: 2026-09-04
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Implementar los checks de Readiness pendientes (payment tape cargado, sin duplicados)

### Executive Summary
El motor de Readiness Checks ahora valida de verdad, antes de distribuir, que hay fondos candidatos sin distribuir (cuando el pool depende de payment tape) y que no se está por duplicar una distribución ya hecha el mismo día para el mismo master trust. Antes, ambas validaciones existían solo como configuración — el motor las reportaba como "no implementadas" y dejaba pasar igual. Además, se cerró un hueco encontrado durante la revisión: el motor ya no permite correr estas precondiciones sobre una config que no esté activada/verificada.

### Technical Summary
- `PaymentTapeLoadedCheck` (nuevo): pasa automático si el pool no depende de payment tape (`ACCOUNT_BALANCE`/`DATA_SOURCE_AGGREGATION`); si depende (`PAYMENT_TAPE`), falla si no hay ningún payment tape sin distribuir dentro de la ventana de días hábiles configurada.
- `NoDuplicateDistributionCheck` (nuevo): falla si ya existe una distribución `active` para el mismo `masterTrustId` en la misma fecha, consultando de solo lectura la tabla `distribution` de `master_trust_servicer` (`MasterServicerDistributionEntity`/`MasterServicerDistributionJPARepository`, nuevos).
- `ReadinessCheckContext` ganó `masterTrustId` y `pool` para que estos checks tengan lo que necesitan.
- `ReadinessCheckSetting` ganó un campo reservado `forceRunOnNonBusinessDay` (persistido, no usado todavía — override futuro de `BusinessDayCheck`).
- **Decisión revisada en esta iteración**: `masterTrustId` nulo en `NoDuplicateDistributionCheck` ahora es `FAILED`, no `PASSED` — una config incompleta no puede asumirse libre de duplicados.
- **Agregado fuera del alcance original**: `RunReadinessChecksUseCase` rechaza (`DistributionConfigNotActiveException`, HTTP 409) correr sobre cualquier config con `status != ACTIVE` — se detectó que nada lo impedía antes.
- Bugfixes de VPR-9745 encontrados en el camino (configurabilidad de `hibernate.hbm2ddl.auto` para tests H2, corrección de un assert) se separaron a su propio branch/PR (#19) en vez de mezclarse acá.

### Phases Completed
- [x] **Phase 1**: Enriquecer el context y reservar el campo de override — `ReadinessCheckContext`/`RunReadinessChecksUseCase`/`ReadinessCheckSetting` actualizados.
- [x] **Phase 2**: `PaymentTapeLoadedCheck` — implementado y testeado (PAYMENT_TAPE con/sin candidatos, otras estrategias auto-pasan).
- [x] **Phase 3**: `NoDuplicateDistributionCheck` — implementado, testeado contra la tabla real de `master_trust_servicer`, más el fold-in de validación de config activa/completa.
