# Domain Layer Conventions

The domain layer contains business logic, models, services, and ports. Located at
`src/main/java/com/getvaas/distribution/engine/domain/`.

## Models (`domain/model/`)

- Use Java **records** for all domain models — immutable value objects
- No framework annotations except `@JsonIgnoreProperties(ignoreUnknown = true)` when needed for deserialization
- Enums go in `domain/model/enums/`

```java
// Do: immutable record
public record AssignmentRule(
        String owner,
        String amountField,
        PoolSource poolSource,
        List<TableFilter> filters
) {}

// Don't: mutable class with getters/setters in domain model
```

## Services (`domain/service/`)

- Annotated with `@Component` and `@RequiredArgsConstructor`
- Stateless — no instance-level mutable state
- Use `@Slf4j` for logging

**Estado actual:** todavía no hay servicios de dominio reales. Cuando se implemente el motor de
distribución, documentar acá los sub-paquetes reales (análogo a `matcher/`, `postmatch/`,
`querybuilder/` de `conciliation-engine-api`) — por ejemplo, algo del estilo `pool/`, `assignment/`,
`ownership/` según cómo se organice el pipeline de las 9 etapas del wizard.

## Ports (`domain/port/`)

- Interfaces that define contracts for infrastructure dependencies
- Ejemplos esperados (a confirmar cuando se implementen): un port hacia la Ownership API, un port de
  queries crudas contra `payments_db` (análogo a `ConciliationQueryPort`).
