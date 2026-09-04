**Created at**: 2026-09-04
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Listar distribution configs con filtros de nombre, master trust y company

### Executive Summary
Ahora se puede listar las distribution configs existentes desde `GET /configs`, filtrando por nombre, master trust o company (combinables), paginado y ordenable — la base para cualquier pantalla de administración que necesite navegar el conjunto de deals configurados sin conocer sus IDs de antemano.

### Technical Summary
- `DistributionConfigJPARepository` ahora extiende `JpaSpecificationExecutor`; `DistributionConfigSpecifications` arma los 4 filtros (`hasName` parcial case-insensitive, `hasMasterTrustId`, `hasCompanyId`, `isActive`) como specs null-safe combinables por AND.
- `ListDistributionConfigsUseCase` arma el `Pageable` (página + tamaño + sort) y delega en el repositorio, mapeando cada entity a domain.
- `GET /configs?name=&masterTrustId=&companyId=&page=&size=&sortBy=&sortDirection=` — todos los parámetros opcionales, `page=0`/`size=20`/`sortBy=name`/`sortDirection=asc` por default.
- **Sort agregado tras revisión** (fuera del alcance original): `sortBy` acepta `name`/`masterTrustId`/`companyId` con lista blanca — un valor inválido responde `400` en vez de un `500` sin controlar o un orden ignorado en silencio.
- Se detectó y corrigió en el camino un bug real de esta versión de Spring Data JPA: `Specification.and()` ya no tolera un operando `null` (`IllegalArgumentException`) — resuelto con una spec `NO_OP` (`cb.conjunction()`) para los filtros ausentes.
- Primer `@DataJpaTest` del repo — requirió resolver la reubicación de paquetes de Spring Boot 4 (`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`) y parametrizar `hibernate.hbm2ddl.auto` (antes hardcodeado a `none`) para que H2 pueda crear el schema en tests.

### Phases Completed
- [x] **Phase 1**: Repositorio y Specifications — `JpaSpecificationExecutor`, 4 specs null-safe, `@DataJpaTest` contra H2.
- [x] **Phase 2**: Use case y DTOs — `ListDistributionConfigsUseCase`, `ListDistributionConfigsRequest`, `DistributionConfigListResponse`.
- [x] **Phase 3**: Router — `GET /configs` con filtros y paginación.
- [x] **Phase 4**: Sort — `sortBy`/`sortDirection` con lista blanca validada, agregado tras revisión.
