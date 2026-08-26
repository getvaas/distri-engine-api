# Project Structure

```
distri-engine-api/
├── build.gradle                    # Gradle build (Spring Boot 4, Java 21)
├── example.env                     # Environment variable template
├── scripts/
│   ├── run-tests.sh                # Docker-based test runner
│   └── clean-test-cache.sh         # Test cache cleanup
├── docs/                           # Project documentation (arquitectura + negocio)
└── src/
    ├── main/
    │   ├── java/com/getvaas/distribution/engine/
    │   │   ├── DistributionEngineApplication.java
    │   │   ├── application/
    │   │   │   └── usecase/            # One class per use case (vacío todavía)
    │   │   ├── domain/
    │   │   │   ├── model/              # Records (value objects) + enums/
    │   │   │   ├── port/               # Interfaces for external dependencies
    │   │   │   └── service/            # Domain logic
    │   │   └── infrastructure/
    │   │       ├── client/             # External API clients (Auth0, Company API, Ownership API, etc.)
    │   │       ├── config/             # Spring configuration beans (ya pobladas — ver abajo)
    │   │       ├── persistence/
    │   │       │   ├── payments/       # JPA repos, entities, mappers sobre payments_db (payment_tape, etc.)
    │   │       │   └── engine/         # Esquema propio del motor (distribution_config, etc. — a definir)
    │   │       └── web/
    │   │           ├── dto/            # Request/response records
    │   │           └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.yml         # Base config
    │       ├── infra-config.yml        # Server, datasource, external services
    │       └── local.env-example       # Local env template
    └── test/
        ├── java/                       # Unit tests (mirrors main structure)
        └── resources/
            └── application.properties  # H2 in-memory config for tests
```

## Config classes ya creadas (`infrastructure/config/`)

| Clase | Rol |
|---|---|
| `PaymentsDataSourceConfig` | Datasource + JPA hacia `payments_db` (MySQL), paquete `infrastructure.persistence.payments` |
| `JacksonConfig` | `ObjectMapper` global con soporte de `java.time` |
| `SecurityConfig` | Integra `vaas-security-lib`, hoy `permitAll()` hasta que se definan los endpoints reales |
| `AsyncConfig` | Thread pool `distributionExecutor` (2-5 threads) para ejecución async |
| `RedisConfig` | Conexión Redis (Lettuce), compartida a nivel Vaas |
| `ShedLockConfig` | Lock distribuido vía Redis para jobs/cron (prefijo `distribution-engine:shedlock`) |
| `SnsConfig` | Cliente AWS SNS para eventos de dominio |

**Pendiente, no creado todavía** (agregar cuando se implemente el negocio):
- El datasource propio del motor (`persistence/engine/`, análogo a como `conciliation-engine-api` maneja
  sus tablas `conciliation_engine_config`/`conciliation_execution`) — la convención exacta de wiring
  (datasource nombrado vs. default) hay que confirmarla contra el repo hermano antes de copiarla, no está
  100% clara desde los archivos visibles ahí.
- `GlobalExceptionHandler` está vacío — se le agregan `@ExceptionHandler` a medida que existan excepciones
  de dominio reales.
