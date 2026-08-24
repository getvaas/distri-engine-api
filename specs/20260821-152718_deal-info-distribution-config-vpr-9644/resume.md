**Created at**: 2026-08-21
**Based on plan**: @specs/20260821-152718_deal-info-distribution-config-vpr-9644/plan.md
**Based on story**: @specs/20260821-152718_deal-info-distribution-config-vpr-9644/story.md

# Resume: Deal Info — persistir la config base de una distribución

### Executive Summary
Se construyó la base de datos y API para crear y editar la configuración de un deal de distribución
(borrower, master trust, país, moneda, nombre). Es el cimiento sobre el que se construyen las siguientes
8 etapas del wizard — sin esto, ninguna otra parte de la configuración tiene dónde persistirse.

### Technical Summary
- Nueva tabla `distribution_engine_config`, reusando el datasource `payments-db` ya existente (sin
  necesidad de un segundo datasource, confirmado contra el patrón real de `conciliation-engine-api`).
- Patrón "entidad delgada + config_json" — igual al de `conciliation_engine_config` y `distribution_config`
  (Kotlin) ya en producción.
- `@JsonIgnoreProperties(ignoreUnknown = true)` en el payload — corrige un riesgo real detectado en el
  `DistributionConfigPayload` de Kotlin, que no lo tiene.
- Endpoints sin `@VaasSecurity` por ahora (a pedido, para probar sin fricción) — queda un TODO explícito
  en el router para reactivarlo.
- 9 tests unitarios, 0 fallas.

### Phases Completed
- [x] **Phase 1**: Modelo de dominio + entidad + mapper — records + entidad JPA + mapper MapStruct/Jackson.
- [x] **Phase 2**: Use cases + endpoints — CRUD mínimo (create/get/update) de Deal Info.
- [x] **Phase 3**: Tests — mapper + 3 use cases, Mockito puro.
