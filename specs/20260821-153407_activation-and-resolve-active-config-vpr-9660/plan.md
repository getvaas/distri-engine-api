**Created at**: 2026-08-21
**Status**: Done
**Based on story**: @specs/20260821-153407_activation-and-resolve-active-config-vpr-9660/story.md

# Plan: Activar una config y resolver la activa para ejecutar

### Goal
Agregar la transición `DRAFT/INACTIVE → ACTIVE` (desactivando cualquier otra config activa del mismo
borrower) y un mecanismo de resolución por `companyId` que sea la única fuente de verdad para "cuál
config uso para distribuir".

### Context
- `DistributionEngineConfigEntity` / `DistributionConfigJPARepository` (VPR-9644) — se les agrega una
  query nueva por `companyId` + `status`.
- `UpdatePoolConfigUseCase` (VPR-9628) — mismo patrón de "mutar la entidad gestionada + guardar" a reusar
  para la activación.
- `docs/proceso-distribucion-unificado.md` Sección 1, paso 1-2 — `BorrowerConfigResolver.getByBorrowerCode()`
  es el equivalente real en `master-trust-servicer-api` de lo que este ticket construye acá.

### Public Contracts
- **Repository**: `findByCompanyIdAndStatusAndDeletedFalse(Long companyId, DistributionConfigStatus status)`.
- **Use cases**: `ActivateDistributionConfigUseCase.execute(String id)`,
  `ResolveActiveDistributionConfigUseCase.execute(Long companyId)`.
- **Exceptions**: `NoActiveDistributionConfigException` (404), `MultipleActiveDistributionConfigException` (409).
- **Endpoints**: `POST /configs/{id}/activate`, `GET /configs/active?companyId=`.
- **Tests**: `ActivateDistributionConfigUseCaseTest`, `ResolveActiveDistributionConfigUseCaseTest`.

### Phases

#### Phase 1: Activación
[Permite que una config llegue a ACTIVE, garantizando que nunca haya dos al mismo tiempo.]
- [x] Query de repositorio por `companyId` + `status`
- [x] `ActivateDistributionConfigUseCase` — activa la target, desactiva cualquier otra ACTIVE del mismo
  `companyId`
- [x] Endpoint `POST /configs/{id}/activate`

#### Phase 2: Resolución
[El mecanismo que el motor de ejecución va a llamar para arrancar una corrida.]
- [x] `ResolveActiveDistributionConfigUseCase` — 0 activas → 404, 1 activa → la devuelve, 2+ activas → 409
- [x] `NoActiveDistributionConfigException`, `MultipleActiveDistributionConfigException` + handlers
- [x] Endpoint `GET /configs/active?companyId=`

#### Phase 3: Tests
[Cubre la transición de estados y los 3 escenarios de resolución.]
- [x] Activar sin config previa activa
- [x] Activar desactivando a la anterior
- [x] Resolver: 0 / 1 / 2+ activas

### Next Step
Completado — el resto del Bloque 1 (trigger real, sea BPM o endpoint) y el Bloque 2 (ventana de búsqueda,
gate de conciliación) quedan para tickets siguientes; ya tienen dónde apoyarse.
