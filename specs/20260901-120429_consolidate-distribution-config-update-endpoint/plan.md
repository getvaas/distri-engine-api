**Created at**: 2026-09-01
**Status**: Draft
**Based on story**: @story.md

# Plan: Consolidar la creación y actualización de DistributionConfig en endpoints full-payload

### Goal
Reemplazar los 7 `PUT /configs/{id}/{node}` por bloque (pool, payment-filters, distribution-rules,
ownership, readiness-checks, notifications, transfer-instructions, virtual-columns) por un único
`PUT /configs/{id}` (y opcionalmente `POST /configs`) que reciba la estructura completa del config
y la persista como fuente de verdad, sin merge implícito contra el valor anterior — preservando
exactamente las mismas reglas de validación/defaults por nodo que existen hoy.

### Context
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/DistributionConfigRouter.java` — router a modificar: quitar 7 `@PutMapping` por bloque.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/Update{Pool,PaymentFilters,DistributionRules,Ownership,ReadinessChecksConfig,Notifications,TransferInstructions,VirtualColumns}UseCase.java` — 8 clases con la lógica de validación/build a extraer y luego eliminar.
- `src/main/java/com/getvaas/distribution/engine/application/usecase/{Create,Update}DistributionConfigUseCase.java` — se reescriben para orquestar los 8 builders.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/dto/{Create,Update}DistributionConfigRequest.java` — ganan 8 campos opcionales.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/dto/Update{Pool,PaymentFilters,DistributionRules,Ownership,ReadinessChecksConfig,Notifications,TransferInstructions,VirtualColumns}Request.java` — se reusan tal cual como tipo de esos 8 campos nuevos, sin modificar.
- `src/main/java/com/getvaas/distribution/engine/domain/model/DistributionConfigPayload.java` — record de los 9 nodos, sin cambios de forma.
- `src/test/java/com/getvaas/distribution/engine/application/usecase/Update*UseCaseTest.java` (8 archivos) — se migran a tests de builder y luego se eliminan.
- `docs/architecture/distribution-config-schema.md` — sección "Endpoints por nodo" a actualizar al final.

### Public Contracts
- **Services**:
  - `PoolConfigBuilder.build(UpdatePoolConfigRequest) -> PoolConfig`
  - `PaymentFiltersConfigBuilder.build(UpdatePaymentFiltersRequest) -> PaymentFiltersConfig`
  - `DistributionRulesConfigBuilder.build(UpdateDistributionRulesRequest) -> DistributionRulesConfig`
  - `OwnershipConfigBuilder.build(UpdateOwnershipRequest) -> OwnershipConfig`
  - `ReadinessChecksConfigBuilder.build(UpdateReadinessChecksConfigRequest) -> ReadinessChecksConfig`
  - `NotificationsConfigBuilder.build(UpdateNotificationsRequest) -> NotificationsConfig`
  - `TransferInstructionsConfigBuilder.build(UpdateTransferInstructionsRequest) -> TransferInstructionsConfig`
  - `VirtualColumnsConfigBuilder.build(UpdateVirtualColumnsRequest) -> VirtualColumnsConfig`
  - `UpdateDistributionConfigUseCase.execute(String id, UpdateDistributionConfigRequest) -> DistributionConfig` (reescrito: cada nodo presente en el request se construye vía su builder; ausente/null → `null`, sin fallback a `existing`; Deal Info conserva fallback)
  - `CreateDistributionConfigUseCase.execute(CreateDistributionConfigRequest) -> DistributionConfig` (reescrito: mismo criterio de construcción por nodo que el update)
- **Tests**:
  - `PoolConfigBuilderTest`, `PaymentFiltersConfigBuilderTest`, `DistributionRulesConfigBuilderTest`, `OwnershipConfigBuilderTest`, `ReadinessChecksConfigBuilderTest`, `NotificationsConfigBuilderTest`, `TransferInstructionsConfigBuilderTest`, `VirtualColumnsConfigBuilderTest` — migran los casos de validación de los `Update*UseCaseTest` actuales, sin mocks (builders son funciones puras).
  - `UpdateDistributionConfigUseCaseTest` — casos nuevos: nodo presente delega al builder correspondiente; nodo ausente/null resulta en `null` (no preserva `existing`); Deal Info con campos null preserva `existing`.
  - `CreateDistributionConfigUseCaseTest` — casos nuevos: request sin nodos crea payload con todos null (comportamiento actual, sin regresión); request con nodos presentes los construye vía builder.
- **Database**: sin cambios — `config_json` mantiene exactamente la misma forma serializada.

### Phases

#### Phase 1: Extraer los 8 builders sin tocar el router
Los 8 `Update*UseCase` delegan su lógica de construcción a un builder nuevo, manteniendo endpoints y comportamiento actuales idénticos — paso intermedio de bajo riesgo, con red de tests existente intacta.
- [ ] Crear `PoolConfigBuilder` con la lógica de `UpdatePoolConfigUseCase.buildAccountBalanceConfig` + selección de estrategia/defaults, y su test `PoolConfigBuilderTest` (casos migrados de `UpdatePoolConfigUseCaseTest`).
- [ ] Crear `PaymentFiltersConfigBuilder` con la lógica de `UpdatePaymentFiltersUseCase` (accountingPayments, gatewayFilters, conciliationRequirements, dateTimeFilters) + `PaymentFiltersConfigBuilderTest`.
- [ ] Crear `DistributionRulesConfigBuilder` con la lógica de `UpdateDistributionRulesUseCase` + `DistributionRulesConfigBuilderTest`.
- [ ] Crear `OwnershipConfigBuilder` con la lógica de `UpdateOwnershipUseCase` + `OwnershipConfigBuilderTest`.
- [ ] Crear `ReadinessChecksConfigBuilder` con la lógica de `UpdateReadinessChecksConfigUseCase` (incluye defaults de los 3 checks) + `ReadinessChecksConfigBuilderTest`.
- [ ] Crear `NotificationsConfigBuilder` con la lógica de `UpdateNotificationsUseCase` (channels, templates, sftpDelivery) + `NotificationsConfigBuilderTest`.
- [ ] Crear `TransferInstructionsConfigBuilder` con la lógica de `UpdateTransferInstructionsUseCase` + `TransferInstructionsConfigBuilderTest`.
- [ ] Crear `VirtualColumnsConfigBuilder` con la lógica de `UpdateVirtualColumnsUseCase` + `VirtualColumnsConfigBuilderTest`.
- [ ] Actualizar los 8 `Update*UseCase` para delegar en su builder (constructor injection), sin cambiar su `execute()` público ni el router. Tests viejos de estos 8 use cases siguen pasando sin modificar sus aserciones.

#### Phase 2: Consolidar Create/Update para recibir el payload completo
`PUT /configs/{id}` y `POST /configs` pasan a aceptar los 8 nodos completos y a construirlos vía los builders de la Fase 1, sin merge contra `existing`.
- [ ] Agregar los 8 campos opcionales a `UpdateDistributionConfigRequest` y a `CreateDistributionConfigRequest` (reusando los records `Update*Request` existentes).
- [ ] Reescribir `UpdateDistributionConfigUseCase`: inyectar los 8 builders; por cada nodo, `request.nodo() != null ? builder.build(request.nodo()) : null`; Deal Info conserva el fallback a `existing` que tiene hoy.
- [ ] Reescribir `CreateDistributionConfigUseCase`: inyectar los 8 builders; mismo criterio de construcción por nodo (sin `existing`, porque es alta).
- [ ] Actualizar `UpdateDistributionConfigUseCaseTest` y `CreateDistributionConfigUseCaseTest` con los casos de la sección Public Contracts (mocks de builders + AssertJ).

#### Phase 3: Eliminar los endpoints y clases por bloque
El router y el código fuente quedan con un único punto de entrada de escritura por operación.
- [ ] Quitar del router los 7 `@PutMapping("/{id}/{node}")` y sus 7 campos de use case inyectados.
- [ ] Eliminar las 8 clases `Update*UseCase` (ya redundantes, su lógica vive en los builders + el use case consolidado).
- [ ] Eliminar los 8 `Update*UseCaseTest.java` viejos (superados por los `*ConfigBuilderTest` + los tests de la Fase 2).

#### Phase 4: Documentación
- [ ] Actualizar `docs/architecture/distribution-config-schema.md`, sección "Endpoints por nodo", reflejando que solo quedan `PUT /configs/{id}` (full payload), `POST /configs` (full payload opcional) y `PUT /configs/{id}/status`.

#### Phase 5 (agregada durante implementación): Sacar el readiness check del API público
`GET /configs/readiness?companyId=&date=` exponía como endpoint REST algo que en realidad es un
paso interno del pipeline de ejecución (Pista B, VPR-9661) — no una consulta de configuración. El
usuario decidió que no debe existir como endpoint público en absoluto.
- [ ] `RunReadinessChecksUseCase.execute` pasa a recibir `(String id, LocalDate date)` y resuelve la
      config vía `GetDistributionConfigUseCase` (no `ResolveActiveDistributionConfigUseCase`),
      obteniendo `companyId` del propio `DistributionConfig` cargado — la clase se conserva como
      pieza interna reusable por el motor de ejecución cuando se construya.
- [ ] Se quita del router el `@GetMapping` de readiness (probado primero como `/{id}/readiness`,
      descartado después: no debe existir como endpoint) y se borra `ReadinessCheckOutcomeResponse`
      (DTO web que ya no tiene consumidor).
- [ ] `RunReadinessChecksUseCaseTest` actualizado para mockear `GetDistributionConfigUseCase`.

Como el resto del plan: implementado y compilando, sin marcar `[x]` hasta confirmar con
`./scripts/run-tests.sh` (pendiente de Docker).

### Next Step
Las 4 fases están implementadas en el working tree de `feature/unify-distribution-config-endpoints`
(builders + delegación, DTOs consolidados, router limpio, docs actualizadas) y compilan limpio
(`./gradlew compileJava compileTestJava`), pero ningún checkbox se marca `[x]` todavía porque no hay
confirmación de tests reales pasando — el Docker daemon que necesita `./scripts/run-tests.sh` no
está corriendo. Próximo paso: correr `./scripts/run-tests.sh` con Docker arriba, resolver lo que
falle, y recién ahí marcar las fases como completas.
