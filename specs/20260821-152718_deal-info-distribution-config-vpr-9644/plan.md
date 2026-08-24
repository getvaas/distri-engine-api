**Created at**: 2026-08-21
**Status**: Done
**Based on story**: @specs/20260821-152718_deal-info-distribution-config-vpr-9644/story.md

# Plan: Deal Info — persistir la config base de una distribución

### Goal
Tener un CRUD mínimo (crear, leer, actualizar) de `DistributionConfig` persistido como entidad+JSON, con
Deal Info (name, companyId, masterTrustId, country, currency) tipado y el resto de las secciones del
wizard como placeholders.

### Context
- `conciliation-engine-api/.../ConciliationEngineConfigEntity.java` — patrón de entidad delgada +
  config_json a replicar.
- `conciliation-engine-api/.../ConciliationEngineConfigMapper.java` — patrón de mapper MapStruct +
  ObjectMapper para (de)serializar el JSON.
- `master-trust-servicer-api/.../DistributionConfig.kt` — el modelo real que ya existe en producción,
  usado como referencia de qué campos son negocio real vs. qué falta.

### Public Contracts
- **Domain**: `DistributionConfig(id, name, companyId, masterTrustId, status, config, createdAt,
  updatedAt, createdBy, updatedBy)`, `DistributionConfigPayload(country, currency, pool,
  distributablePayments, virtualColumns, rules, ownership, readinessChecks, notifications)`.
- **Database**: tabla nueva `distribution_engine_config` en el datasource `payments-db` existente (mismo
  esquema que `payment_tape`, sin datasource propio adicional).
- **Endpoints**: `POST /configs`, `GET /configs/{id}`, `PUT /configs/{id}`.
- **Tests**: `DistributionConfigMapperTest` (round-trip + `ignoreUnknown`), `CreateDistributionConfigUseCaseTest`,
  `GetDistributionConfigUseCaseTest`, `UpdateDistributionConfigUseCaseTest`.

### Phases

#### Phase 1: Modelo de dominio + entidad + mapper
[Establece el patrón de persistencia que van a reusar todas las etapas siguientes del wizard.]
- [x] `DistributionConfigStatus` (DRAFT/ACTIVE/INACTIVE)
- [x] `DistributionConfig` + `DistributionConfigPayload` (records, secciones TBD como `JsonNode`)
- [x] `DistributionEngineConfigEntity` (JPA, tabla `distribution_engine_config`)
- [x] `DistributionConfigMapper` (MapStruct + Jackson, con `@JsonIgnoreProperties(ignoreUnknown=true)`)

#### Phase 2: Use cases + endpoints
[CRUD mínimo expuesto por HTTP.]
- [x] `CreateDistributionConfigUseCase` (arranca en DRAFT)
- [x] `GetDistributionConfigUseCase` (404 si no existe)
- [x] `UpdateDistributionConfigUseCase` (solo Deal Info, preserva el resto)
- [x] `DistributionConfigRouter` + DTOs + `GlobalExceptionHandler`

#### Phase 3: Tests
[Verifica el patrón de persistencia y el CRUD sin necesitar una base real.]
- [x] Mapper: create/round-trip/protección ante campos desconocidos
- [x] Use cases: Mockito puro, AAA, sin JPA real (mismo patrón que `conciliation-engine-api`)

### Next Step
Completado — sirve de base para Pool Strategy (VPR-9628) y las siguientes etapas del wizard.
