# Architecture Principles

This project follows **Clean Architecture** with three distinct layers: domain, application, and infrastructure.
Same convention as `conciliation-engine-api`, el repo hermano de conciliación.

## Layer Structure

```
domain/           -- Pure business logic, no framework dependencies (except Spring stereotypes)
application/      -- Use cases that orchestrate domain logic
infrastructure/   -- Adapters: web (REST), persistence (JPA/JDBC), external clients
```

## Dependency Direction

```
infrastructure --> application --> domain
```

- **Domain** must NOT import from `application` or `infrastructure` (exception: Spring `@Component` annotation).
- **Application** must NOT import from `infrastructure.web` — use cases receive plain parameters, not HTTP objects.
- **Infrastructure** depends on both `application` and `domain`.

## Key Patterns

| Pattern | Convention |
|---|---|
| Use cases | One class per use case in `application/usecase/`, annotated `@Component`, single public `execute()` method |
| Domain models | Java records in `domain/model/` — immutable value objects |
| Domain services | Classes in `domain/service/` — stateless, annotated `@Component` |
| Ports | Interfaces in `domain/port/` — implemented in `infrastructure` |
| REST endpoints | Router classes in `infrastructure/web/`, annotated `@RestController` (named "Router", not "Controller") |
| DTOs | Records in `infrastructure/web/dto/` — request/response objects |
| Entities | JPA entities in `infrastructure/persistence/*/entity/` — Lombok `@Data` + `@Builder` |
| Mappers | MapStruct abstract classes in `infrastructure/persistence/` |
| Soft delete | Preferir `deleted=true` flag sobre hard deletes, siguiendo el patrón de master-trust-servicer-api / conciliation-engine-api |

## Dependency Injection

Spring constructor injection via Lombok `@RequiredArgsConstructor`. No `@Autowired` on fields (except in MapStruct
abstract mappers where `@Autowired protected` is used).

## Estado actual

Este repo es la base de tooling. Todavía no hay entidades ni use cases reales — se agregan
incrementalmente a medida que se implementa cada etapa del wizard de configuración (ver
`docs/distribution-engine-onboarding.html` y las historias VPR-9627 en Jira). Actualizar este documento
y `docs/architecture/project-structure.md` a medida que se agreguen módulos nuevos.
