# Key Files

| File | Purpose |
|---|---|
| `build.gradle` | Dependencies and build config (Spring Boot 4, Java 21, Lombok, MapStruct) |
| `src/main/resources/application.yml` | Base Spring config (nombre de la app, import de infra-config) |
| `src/main/resources/infra-config.yml` | Server, datasource, Redis, Auth0, AWS, seguridad |
| `src/main/resources/logback-spring.xml` | Logging: consola en local/test, CloudWatch + rolling file en dev/stg/prod |
| `src/main/resources/local.env-example` | Template de variables de entorno para desarrollo local |
| `example.env` | Mismo template, en la raíz del repo (usado por Docker/CI) |
| `src/test/resources/application.properties` | Config H2 in-memory para tests |
