# AGENTS.md

Distribution Engine API — a Spring Boot 4 (Java 21) REST API que configura y ejecuta las distribuciones
de fondos de master-trust, reemplazando la lógica de código-por-deal por configuración-por-deal (ver
`docs/epica-distri-engine.md`). Construido con Clean Architecture (domain, application, infrastructure)
usando Gradle, JPA, JDBC, MapStruct y Lombok — mismas herramientas que `conciliation-engine-api`, el
motor de conciliación hermano de este repo.

## Key Commands

| Action | Command |
|---|---|
| Build | `./gradlew build` |
| Run | `./gradlew bootRun` |
| Test (Docker) | `./scripts/run-tests.sh` |
| Test (local) | `./gradlew test` |
| Clean test cache | `./scripts/clean-test-cache.sh` |

## Before Modifying Code

Read the relevant documentation based on what you're about to do:

| What you need to understand             | Read these docs                              |
|------------------------------------------|----------------------------------------------|
| Architecture rules and layer boundaries | `docs/architecture/`                         |
| Code conventions for a specific layer   | `docs/code/[layer-name]/`                    |
| Code style (naming, formatting)         | `docs/code/code-style.md`                    |
| Testing approach and conventions        | `docs/testing/`                              |
| How to run or configure the project     | `docs/how-to-run/`, `docs/configuration/`    |
| El negocio de distribución (qué configurar, por qué) | `docs/epica-distri-engine.md`, `docs/mapeo-borrowers-configuracion.md`, `docs/distribucion-unificada-backlog.md`, `docs/conciliacion-*.md` |

To identify which files are relevant inside each folder, check the file names — they are self-descriptive.

## Estado actual

Este repo es la base de desarrollo (tooling + esqueleto de arquitectura). El negocio (entidades, use
cases, endpoints de configuración de distribución) todavía no está implementado — se construye
iterativamente sobre esta base, siguiendo las 9 etapas del wizard documentadas en
`docs/distribution-engine-onboarding.html` y las historias de VPR-9627 en Jira.
