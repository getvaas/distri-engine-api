# Application Layer Conventions

The application layer contains use cases. Located at
`src/main/java/com/getvaas/distribution/engine/application/usecase/`.

## Use Case Pattern

Each use case is a **single class** with a single public `execute()` method.

```java
@Component
@RequiredArgsConstructor
public class CreateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(CreateDistributionConfigRequest request) {
        // orchestration logic
    }
}
```

### Do / Don't

| Do | Don't |
|---|---|
| One class per use case | Group multiple operations in a "Service" class |
| Name as `VerbNounUseCase` | Name as `NounService` |
| Single `execute()` method | Multiple public methods |
| Use `@Component` | Use `@Service` |
| Throw domain exceptions | Return HTTP status codes |

## Exception Conventions

Custom exceptions live alongside use cases in the same package. Exceptions are plain classes (not
checked). The `GlobalExceptionHandler` maps them to `ProblemDetail` responses.

**Estado actual:** todavía no hay use cases ni excepciones reales — este archivo se completa con la
tabla de excepciones (nombre → HTTP status → cuándo) a medida que se implementan, siguiendo el mismo
patrón que `docs/code/application/conventions.md` de `conciliation-engine-api`.

## Async Execution

Si una ejecución de distribución corre async, seguir el patrón de `conciliation-engine-api`: un
`@Component` separado (no el use case mismo) para que el proxy `@Async` de Spring funcione, delegando al
thread pool `distributionExecutor` (definido en `AsyncConfig`).
