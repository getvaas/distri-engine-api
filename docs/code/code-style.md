# Code Style

Mismas convenciones que `conciliation-engine-api`.

## Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `CreateDistributionConfigUseCase`, `AssignmentResolver` |
| Methods | camelCase | `execute()`, `resolveOwner()` |
| Variables | camelCase | `borrowerCode`, `poolTotal` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_DAYS_BACK`, `MAX_RULES` |
| Packages | lowercase | `domain.model.enums`, `infrastructure.web.dto` |
| Database columns | snake_case | `borrower_code`, `config_json` |
| JSON properties | snake_case | Configured globally via Jackson `SNAKE_CASE` |
| REST endpoints | kebab-case paths | `/distribution-engine`, `/configs/{id}` |

## File Naming

- One public class per file, named after the class
- REST endpoint classes: `*Router.java` (not Controller)
- Use cases: `*UseCase.java`
- Domain exceptions: `*Exception.java` (in same package as use cases)
- DTOs: `*Request.java`, `*Response.java`
- JPA entities: `*Entity.java`
- MapStruct mappers: `*Mapper.java`
- JPA repositories: `*JPARepository.java`

## Lombok Usage

| Annotation | Where Used |
|---|---|
| `@RequiredArgsConstructor` | Use cases, services, routers — for constructor injection |
| `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` | JPA entities |
| `@Slf4j` | Services and engine classes that need logging |

## Records vs Classes

- **Records**: domain models, DTOs, value objects
- **Classes**: JPA entities (need mutability), MapStruct mappers (abstract), Spring components

## Error Handling

- Domain exceptions are unchecked
- `GlobalExceptionHandler` maps exceptions to RFC 7807 `ProblemDetail` responses
- No try-catch in use cases unless needed for specific error translation
