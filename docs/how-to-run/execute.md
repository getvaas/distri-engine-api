# How to Run

## Local

1. Copiar `example.env` a `.env` (o `src/main/resources/local.env-example` a `.env` local) y completar
   las credenciales reales de `payments_db` y Redis.
2. `./gradlew bootRun`
3. La API queda en `http://localhost:8080/api/distribution-engine`, con actuator health en
   `http://localhost:8080/actuator/health`.

## Tests

```
./gradlew test              # local, requiere JDK 21 + Gradle instalados
./scripts/run-tests.sh      # Docker, sin instalar nada localmente (usa volumen cacheado de Gradle)
./scripts/clean-test-cache.sh   # si hay problemas con dependencias cacheadas
```

## Docker

```
docker build -t distribution-engine-api .
docker run -p 8080:8080 --env-file .env distribution-engine-api
```
