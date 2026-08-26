# Infrastructure Layer Conventions

The infrastructure layer contains adapters for web, persistence, external clients, and config.
Located at `src/main/java/com/getvaas/distribution/engine/infrastructure/`.

## Web (`infrastructure/web/`)

### Routers (REST Controllers)

- Named **Router**, not Controller (e.g., `DistributionConfigRouter`)
- Annotated with `@RestController` and `@RequestMapping`
- Delegate to use cases — no business logic in routers

```java
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class DistributionConfigRouter {
    private final CreateDistributionConfigUseCase createConfigUseCase;

    @PostMapping
    public ResponseEntity<ConfigResponse> create(@Valid @RequestBody CreateDistributionConfigRequest request) {
        var config = createConfigUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConfigResponse.from(config));
    }
}
```

### DTOs (`infrastructure/web/dto/`)

- Java **records** for request and response objects
- Request records use Jakarta Validation (`@NotBlank`, `@NotNull`)
- Response records have a `static from(DomainModel)` factory method
- JSON uses `SNAKE_CASE` naming (configured globally via Jackson)

### Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) catches domain exceptions and returns
RFC 7807 `ProblemDetail` responses. Hoy está vacío — completar a medida que existan excepciones reales.

## Persistence (`infrastructure/persistence/payments/`)

### JPA Entities (`entity/`)

- Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Table and column names in `snake_case`
- Nunca modificar el schema existente de `payments_db` desde este repo (`hibernate.hbm2ddl.auto: none`,
  ya configurado en `PaymentsDataSourceConfig`)

### Repositories

- Spring Data JPA interfaces extending `JpaRepository`
- Custom query methods use Spring Data naming conventions (e.g., `findByIdAndDeletedFalse`)

### Mappers

- MapStruct abstract classes with `componentModel = "spring"`

## External Clients (`infrastructure/client/`)

Ejemplos esperados (a implementar según el negocio): cliente a la Ownership API, cliente a la Company
API — mismo patrón que `conciliation-engine-api` (Spring `RestClient`, no `RestTemplate`).

## Configuration (`infrastructure/config/`)

| Class | Purpose |
|---|---|
| `PaymentsDataSourceConfig` | Configures `paymentsDataSource` for the payments_db MySQL |
| `JacksonConfig` | Global Jackson settings (SNAKE_CASE) |
| `SecurityConfig` | Integra `vaas-security-lib`, hoy en modo abierto (`permitAll`) |
| `AsyncConfig` | Thread pool `distributionExecutor` para ejecución async |
| `RedisConfig` | Conexión Redis compartida |
| `ShedLockConfig` | Lock distribuido para jobs/cron |
| `SnsConfig` | Cliente AWS SNS |

**Nota de auditoría:** en `conciliation-engine-api`, la documentación menciona una clase `FlywayConfig`
para migraciones multi-datasource, pero al revisar el código real (`build.gradle` y el árbol de
`infrastructure/config/`) esa clase y la dependencia de Flyway **no existen** — es un desvío entre
documentación y código en el repo hermano. No replicar esa mención acá hasta confirmar cómo se manejan
las migraciones realmente (probablemente vía las migraciones de `master-trust-servicer-api`, que es
dueño del schema `payments_db`).
