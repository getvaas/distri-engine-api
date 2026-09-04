**Created at**: 2026-09-03
**Status**: Draft
**Based on story**: @story.md

# Plan: Listar distribution configs con filtros de nombre, master trust y company

### Goal
Exponer `GET /configs` con filtros opcionales y combinables (`name` parcial, `masterTrustId` exacto,
`companyId` exacto), paginado, excluyendo siempre configs soft-deleted (`active = false`) — VPR-9745.

### Context
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/masterservicer/DistributionConfigJPARepository.java` — gana `JpaSpecificationExecutor`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/persistence/masterservicer/entity/DistributionEngineConfigEntity.java` — campos usados por las specs: `name`, `masterTrustId`, `companyId`, `active`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/DistributionConfigRouter.java` — nuevo `@GetMapping`.
- `src/main/java/com/getvaas/distribution/engine/infrastructure/web/dto/DistributionConfigResponse.java` — se reusa dentro de la respuesta paginada.
- `src/test/resources/application.properties` — H2 en modo MySQL, ya configurado para tests; lo usa el nuevo `@DataJpaTest`.

### Public Contracts
- **Services**:
  - `DistributionConfigSpecifications.hasName(String) -> Specification<DistributionEngineConfigEntity>`
  - `DistributionConfigSpecifications.hasMasterTrustId(Long) -> Specification<DistributionEngineConfigEntity>`
  - `DistributionConfigSpecifications.hasCompanyId(Long) -> Specification<DistributionEngineConfigEntity>`
  - `DistributionConfigSpecifications.isActive() -> Specification<DistributionEngineConfigEntity>`
  - `ListDistributionConfigsUseCase.execute(ListDistributionConfigsRequest) -> Page<DistributionConfig>`
- **Database**: sin cambios de schema — solo nuevas queries sobre `distribution_engine_config`.
- **API**:
  - `GET /configs?name=&masterTrustId=&companyId=&page=0&size=20` → `DistributionConfigListResponse`

### Phases

#### Phase 1: Repositorio y Specifications
- [ ] `DistributionConfigJPARepository` extiende también `JpaSpecificationExecutor<DistributionEngineConfigEntity>`.
- [ ] Crear `DistributionConfigSpecifications` con las 4 specs (`hasName` LIKE case-insensitive, `hasMasterTrustId`, `hasCompanyId`, `isActive`), cada una null-safe (filtro ausente → `Specification.where(null)`/no-op).
- [ ] `DistributionConfigSpecificationsTest` (`@DataJpaTest`, H2): seedea 3-4 entities con distintos `name`/`masterTrustId`/`companyId`/`active`, verifica cada spec sola y combinaciones (AND), y que `active=false` nunca aparece.

#### Phase 2: Use case y DTOs
- [ ] `ListDistributionConfigsRequest(String name, Long masterTrustId, Long companyId, int page, int size)` en `infrastructure/web/dto`.
- [ ] `DistributionConfigListResponse(List<DistributionConfigResponse> items, long totalElements, int totalPages, int page, int size)` con `static from(Page<DistributionConfig>)`.
- [ ] `ListDistributionConfigsUseCase.execute(ListDistributionConfigsRequest) -> Page<DistributionConfig>`: arma `Pageable` (`PageRequest.of(page, size)`), combina las specs presentes, llama `repository.findAll(spec, pageable)`, mapea cada entity a domain vía `DistributionConfigMapper.toDomain`.
- [ ] `ListDistributionConfigsUseCaseTest` (Mockito): request sin filtros devuelve todas (activas); con filtros combinados delega correctamente; pagina bien (verifica `Pageable` armado).

#### Phase 3: Router y docs
- [ ] `GET /configs` en `DistributionConfigRouter`, con `@RequestParam` opcionales y defaults `page=0`, `size=20`.
- [ ] Actualizar `docs/architecture/distribution-config-schema.md` si corresponde (nueva entrada en la tabla de endpoints) — evaluado: ese doc es específicamente sobre la forma de `config_json` (Pista A), no un catálogo general de endpoints; no se actualiza acá.

### Next Step
Las 3 fases están implementadas en el working tree (rama `feature/VPR-9745-list-distribution-configs-with-filters`) y compilan limpio (`./gradlew compileJava compileTestJava`). Ningún checkbox se marca `[x]` todavía: `./scripts/run-tests.sh` falló por un 401 al resolver `com.getvaas:security:v1.0.4` desde GitHub Packages dentro del contenedor Docker — el PAT en `~/.gradle/gradle.properties` parece vencido, no es un problema de este código. Próximo paso: renovar el token, correr `./scripts/run-tests.sh` (primera vez que corre un `@DataJpaTest` en este repo, confirmar que el context de Spring con datasource dual arranca bien), y recién ahí marcar las fases como completas.
