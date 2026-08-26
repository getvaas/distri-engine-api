# Conciliación en `master-trust-servicer-api` (Kotlin) — lo común y las particularidades por borrower

> **Fecha:** 2026-08-19 · **Alcance:** solo el repo `master-trust-servicer-api` (Kotlin, motor config-driven).
> Este es exactamente el documento que [conciliacion-distribucion-estado-actual-por-borrower.md](conciliacion-distribucion-estado-actual-por-borrower.md)
> deja fuera de su alcance ("el motor config-driven de `master-trust-servicer-api` está fuera de este
> documento") — la pieza que faltaba de la trilogía junto con ese doc y
> [conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md](conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md).
> **Fuentes verificadas contra código:** `core/conciliation/*`, `infra/repository/**/conciliation/**`,
> `infra/repository/utils/conciliation/ConciliationRepositoryHelper.kt`, `core/config/BusinessConfig.kt`,
> `core/config/MasterServicerConfig.kt`, `core/distribution/v2/calculator/DistributorCalculator.kt`,
> `src/main/resources/business.yml`, y las migraciones `payments_db/V1.0.31` a `V1.0.35`.

---

## Resumen ejecutivo

En este repo, **"conciliación" no concilia nada — solo reporta sobre un flag que ya fue puesto por otro
sistema.** No hay matching, no hay tolerancia, no hay gate. Todos los repositorios de conciliación son de
**solo lectura** (`@Repository("conciliationReadRepository")`, sin ningún INSERT/UPDATE en el código). El
verdadero matching pasa por otro lado: por los scripts de `master-servicer-apps` (ver el doc de ese repo)
o, a futuro, por el "Conci Engine" cuyo esquema (`conciliation_engine_config`, `conciliation_execution`,
`conciliation_match_results`) ya existe en las migraciones de este mismo repo (`V1.0.31`–`V1.0.35`,
2026-05-29) pero **no tiene ni una sola clase Kotlin que lo use** — es una tabla sembrada a la espera de
implementación, no un motor en producción.

Lo segundo, y más importante para una feature que quiera unificar esto: **hay tres superficies de
configuración distintas que dicen si un borrower "tiene conciliación bancaria", y ninguna es la fuente
única de verdad.** Dos de ellas declaran flags (`borrowers-core`, `payment-tape`) que el código **nunca
lee** — están en el YAML pero no están enlazados a ninguna propiedad Kotlin. La tercera (la que sí
importa para que la plata se mueva) vive en `distribution_config.config_json` en la base de datos, no en
YAML, y es un filtro binario silencioso, no un gate con umbral.

---

## Parte 1 — Lo común

### Arquitectura: una capa de reporting, no un motor de matching

Todo el módulo `core/conciliation` gira sobre un único hecho ya resuelto en la tabla `payments_db.payments`
(y `payment_tape`, `disbursements`, `funds_transfers`): cada fila tiene columnas de FK que indican si ya
fue conciliada (`payment_tape_conciliation_id`, `borrower_db_payment_id`, `fund_transfer_id`, etc.). Este
repo nunca escribe esas columnas — solo las lee y agrega totales. Confirmado por:
- `NativeSQLConciliationReadRepository`, `NativeSQLConciliationHistoryReadRepository`,
  `NativeSQLConciliationMetricsReadRepository`: los tres son `@Repository`, todos con queries `SELECT`
  únicamente, cero `INSERT`/`UPDATE` en todo `infra/repository/**/conciliation/**`.
- La tabla `conciliations` (creada por otro sistema) se lee vía `getById`/`getLastConciliationBy`/
  `getConciliationsByCreationDate` en `NativeSQLConciliationReadRepository.kt` — nunca se persiste desde
  aquí.

### Los 3 "tipos" de conciliación (en realidad 2, con nombres duplicados)

`ConciliationType` (`core/conciliation/Model.kt:15-47`) declara 5 valores pero solo 3 códigos de
persistencia distintos — dos pares son alias exactos del mismo `persistence_code`:

| Enum | `persistence_code` | Qué compara | Quién lo usa hoy |
|---|---|---|---|
| `PAYMENTS_VS_BORROWERS_CORE` / `BORROWERS_CORE_VS_PAYMENTS` | `PAYMENTS___VS___BORROWER_DB` | Payments vs. la BD interna del borrower | Solo ADDI (y ADDI_BNPN) |
| `PAYMENTS_VS_PAYMENT_TAPE` / `PAYMENT_TAPE_VS_PAYMENTS` | `PAYMENTS___VS___PAYMENT_TAPE` | Payments vs. `payment_tape` | La mayoría de borrowers |
| `PAYMENTS_VS_BANK` | `PAYMENTS___VS___BANK` | Payments vs. extractos/`funds_transfers` | Borrowers con conciliación bancaria habilitada |

El `type` no lo decide el sistema por borrower: lo pasa el **caller como path variable**
(`ConciliationController.kt:33`, `:73` — `@PathVariable(value = "type")`). El backend no valida que el tipo
solicitado tenga sentido para ese borrower; simplemente ejecuta la query correspondiente.

### Endpoints y flujo (lo que sí es 100% común)

Un único controller, `ConciliationController` (`/master-trusts/conciliations`), expone 2 operaciones para
cualquier borrower/tipo:

1. **`GET /{type}/history`** — `ConciliationHistoryManager.getHistory()`: totales agrupados por fecha y
   gateway, con relleno de gateways faltantes a partir de `businessConfig.getBorrowerConfig(company)
   .getActiveGatewaysByCompany(...)` para que la respuesta siempre tenga una fila por gateway activo del
   borrower, aunque no haya datos.
2. **`GET /{type}/raw-data`** — `ConciliationRawDataBuilder.buildAndGetDocument()`: genera un Excel
   (`SheetBuilder`) de forma **asíncrona**, lo sube a S3 y devuelve una URL presignada. El patrón es
   idéntico para todos los borrowers:
   - Idempotencia por clave compuesta (`company_id` + `conciliation_type` + rango de fechas + gateways +
     `reconciled`) contra un `KeyValueStoreProvider` (Redis), no por reintento del cliente.
   - Estados: `PENDING → SUCCESS/ERROR/INTERRUPTED/EXPIRED/BUILD_TIMEOUT`, con timeout de 15 min y TTL de
     documento de 240 min (`ConciliationRawDataBuilder.kt:37-39`).
   - El cliente hace polling sobre el mismo endpoint hasta ver `SUCCESS`.

Además hay un tercer endpoint de métricas (`ConciliationMetricsController`, no leído en detalle aquí) que
expone `getTotalsByGateways` / `getPerformance` / `getMonthEnd` de `ConciliationMetricsManager` — el
"month-end" es el reporte más pesado: corre una tarea async por gateway (`coroutineScope` +
`Semaphore(3)`) sumando pagos, no-conciliados y (si aplica) desembolsos bancarios del mes.

### Caching común: Redis, TTL 70 min, con cache-warming programado

Todos los cálculos de month-end pasan por una cache Redis (`ApplicationCacheConfig.CONCILIATION_MONTH_END_CACHE`,
mencionada también en `CLAUDE.md` raíz como "70-min TTL"), con clave
`{company_id}:{company_code}:{gateway}:{year}{month}:{tz}:{métrica}`
(`ConciliationMetricsManager.buildCacheKey`). Un job separado,
`ConciliationMetricsMonthEndCacheRefresher` (`infra/job/conciliation/metrics/monthend/`), corre cada hora
(ShedLock, `lockAtLeastFor=15m`) y **precalienta la cache solo para una allowlist de company IDs**
(`master-servicer-common.conciliation.metrics.month-end.cache.must-refresh-periodically`), iterando 3
timezones fijos (`-05:00`, `-03:00`, `-06:00` — Colombia/México y Argentina) y los últimos 2 meses. Está
desactivado en el perfil `default` (`@Profile("!default")`) — no corre en local.

### El único puente real entre conciliación y distribución: filtros binarios, no un gate

La conciliación **no bloquea nada** en este repo. Lo único que conecta el estado "conciliado" con el motor
de distribución son dos flags booleanos por `distribution_config` (no por `business.yml`):
`config.distributablePayments.gatewayConciliationRequired` y `.bankConciliationRequired`
(`core/distribution/v2/calculator/DistributorCalculator.kt:111-112`), que se traducen en predicados SQL
armados por `ConciliationRepositoryHelper` (`infra/repository/utils/conciliation/ConciliationRepositoryHelper.kt`):
por ejemplo `p.payment_tape_conciliation_id IS NOT NULL` para "concilió contra tape", o una expresión
`CASE` por gateway (con/sin desembolsos) para "concilió contra banco". Un pago que no cumple estas
condiciones **queda fuera del pool de distribución en silencio** — no genera alerta, no cuenta un
porcentaje, no tiene umbral de tolerancia. `grep -rn "tolerance\|threshold\|umbral" core/distribution/` no
devuelve resultados.

---

## Parte 2 — Particularidades por borrower

### La configuración declarada en `business.yml` (3 flags por borrower)

Cada borrower tiene un bloque `business.borrower.<CÓDIGO>.conciliation` con tres sub-flags:
`borrowers-core.enabled`, `payment-tape.enabled`, `bank.enabled` (+ `bank.start-date` opcional). Estado
real de cada uno, leído directo de `business.yml`:

| Borrower | `borrowers-core` | `payment-tape` | `bank` | `bank.start-date` |
|---|---|---|---|---|
| ADDI / ADDI_BNPN | ✅ | ❌ | ✅ | 2025-01-01 |
| SISTECREDITO | ✅ | ✅ | ✅ | 2026-01-01 |
| SOMOS | ❌ | ✅ | ✅ | 2025-01-01 |
| DELTACREDIT | ❌ | ✅ | ✅ | — |
| CREDIORBE | ❌ | ❌ | ✅ | — |
| BIA, WELLI, PAYJOY, FINMAQ, GOBRAVO, INKLUSIVA, NIKO | ❌ | ✅ | ❌ | (algunos con `start-date` pese a `enabled:false`) |
| SOLVENTO | ❌ | ✅ | ❌ | — |
| RAPICREDIT, JTP, WIMO, CREDI7, PLATAFORM, LIQUITECH | ❌ | ❌ | ❌ | (varios con `start-date` "muerto") |

**Hallazgo verificado — dos tercios de este cuadro no hacen nada.** `BusinessConfig.kt:101-106` define
`ConciliationConfig` con **un único campo**: `bankConfig` (`ConciliationBankConfig(enabled, startDate)`).
Los YAML keys `borrowers-core` y `payment-tape` **no están enlazados a ninguna propiedad** de la clase —
Spring los ignora silenciosamente al bindear `@ConfigurationProperties(prefix = "business")`. Es decir: de
las tres columnas de la tabla de arriba, **solo la columna `bank` tiene efecto real** (ver siguiente
sección). El resto es documentación que quedó de un diseño anterior, o pensada para un consumidor externo
(¿frontend?) que hoy no está en este repo.

### La segunda superficie de configuración: `master-servicer.<borrower>.conciliation.bank`

Existe un **segundo** árbol de configuración, con prefijo `master-servicer` (bloque en minúsculas,
`business.yml:712` en adelante — `addi:`, `bia:`, `wimo:`, etc.), leído por `MasterServicerRawConfig`
(`core/config/MasterServicerConfig.kt:91-246`) como un `Map<String, Any>` crudo (no tipado por Spring
binding). De ahí también solo se extrae `conciliation.bank.enabled`
(`MasterServicerRawConfig.getConciliationConfig`, líneas 225-246) — los mismos `borrowers-core` /
`payment-tape` que puedan aparecer en ese bloque también se ignoran.

**Esto significa que "¿tiene este borrower conciliación bancaria?" se puede responder desde dos lugares
distintos** (`business.borrower.X.conciliation.bank.enabled` vía `BusinessConfig`, usado por
`ConciliationRepositoryHelper` y las queries de historial/raw-data; y `master-servicer.x.conciliation.bank.enabled`
vía `MasterServicerConfigLoader`, usado únicamente por `ConciliationMetricsManager.getMonthEnd()` para la
variable `bankConciliationEnabled`) — sin garantía de que ambos coincidan para el mismo borrower, porque
son dos árboles YAML mantenidos a mano por separado.

### El caso ADDI: hardcoded, no config-driven

`ConciliationMetricsManager.getMonthEnd()` decide qué `ConciliationType` usar con un chequeo de
**company ID literal**, no de config ni de borrower:

```kotlin
val conciliationType: ConciliationType = if (companyId in listOf("1", "154"))
    // ADDI oriented. This is the only case like this and will not be more.
    ConciliationType.PAYMENTS_VS_BORROWERS_CORE
else ConciliationType.PAYMENTS_VS_PAYMENT_TAPE
```
(`ConciliationMetricsManager.kt:337-340`). El comentario ("this is the only case like this and will not be
more") ya fue falso una vez — es el tipo de deuda que una feature de unificación normalmente hereda sin
darse cuenta.

También en `ConciliationHistoryManager.getHistory()` hay un comentario explícito reconociendo que ADDI es
un caso especial tratado manualmente: al agrupar por fecha en lugar de por conciliación real, porque "ADDI
concilia el mes entero" y mostrar la conciliación real generaría números gigantes por día
(`ConciliationHistoryManager.kt:64-69`). El código real que haría lo correcto (agrupar por `Conciliation`)
está comentado en el propio archivo, líneas 70-84, con una nota de que se reactivará "cuando todos los
borrowers tengan el proceso de conciliación normalizado".

### FINAMCO: falta en `business.borrower`, no falta en `master-servicer`

`FINAMCO` **no tiene entrada** en `business.borrower.*` (los 17 borrowers del enum + `GOBRAVO`, `INKLUSIVA`,
`NIKO`, `XIMPLE`, `YUPPI` sí la tienen; `FINAMCO` no aparece en ningún punto de ese bloque en
`business.yml`). Sí tiene entrada en el segundo árbol, `master-servicer.finamco`
(`business.yml:998-1012`, con `country: COL`, `master-trust-id: 35`, `conciliation.payment-tape.enabled:
false`, `conciliation.bank.enabled: false`). Como `BusinessConfig.getBorrowerConfig()` lanza
`IllegalStateException` si el borrower no está en `business.borrower`
(`BusinessConfig.kt:27-34`), cualquier llamada a conciliación (`history`, `raw-data`, `getActiveGatewaysByCompany`)
para la compañía FINAMCO fallaría con esa excepción — **no verificado en runtime**, pero es una lectura
directa y consistente del código, no una inferencia.

### CREDIORBE y DELTACREDIT: los únicos con conciliación bancaria "completa" fuera de ADDI/SISTECREDITO/SOMOS

De los borrowers con arquitectura "scrapper" (WIMO, CREDI7, LIQUITECH, RAPICREDIT, JTP, PLATAFORM), ninguno
tiene ninguno de los tres flags en `true` — consistente con la categoría C/D del documento de
`master-servicer-apps`: para esos borrowers la conciliación (cuando existe) corre enteramente fuera de este
repo, y aquí ni siquiera se reporta.

---

## Parte 3 — Qué le pediría un motor de conciliación unificado (para la épica)

1. **Una sola fuente de verdad para "¿qué tipo de conciliación aplica a este borrower?"** Hoy hay tres
   candidatas (`business.borrower.*.conciliation`, `master-servicer.*.conciliation`,
   `distribution_config.config_json.distributablePayments.*ConciliationRequired`) y ninguna es autoritativa
   sobre las otras dos. Cualquier feature nueva debería elegir una y deprecar las otras dos explícitamente,
   no agregar una cuarta.
2. **Eliminar los flags muertos o conectarlos.** `borrowers-core.enabled` y `payment-tape.enabled` en YAML
   dan una falsa sensación de configurabilidad — hoy no cambian ningún comportamiento. O se cablean de
   verdad (p. ej. para validar qué `type` es válido pedir por borrower, algo que el controller no hace hoy)
   o se borran para no confundir a quien intente usarlos como palanca de una nueva feature.
3. **El "Conci Engine" ya tiene tablas pero cero código.** `conciliation_engine_config` /
   `conciliation_execution` / `conciliation_match_results` (`V1.0.31`, `V1.0.32`, `V1.0.35`) están listas
   para un motor real de matching con configuración por borrower (`config_json`), ejecuciones auditables
   (`trigger`, `status`, `match_rate`) y resultados de match trazables (`match_type`: 1:1/1:N/N:1/SUBSET).
   Es la base natural para migrar el matching que hoy vive disperso en `master-servicer-apps`
   (Inklusiva/Niko modelo de referencia, según el otro doc) sin reinventar el esquema.
4. **El gate real (E8 de la épica) necesita más que el flag binario actual.** Hoy
   `gatewayConciliationRequired`/`bankConciliationRequired` de `distribution_config` excluye pagos no
   conciliados del pool **en silencio**. Para el gate por umbral que E8 propone (frenar la distribución si
   más de X% no concilió, con aviso), el predicado SQL ya existe en
   `ConciliationRepositoryHelper.getPaymentTapeVsPaymentsReconciledCondition` — falta contar sobre el pool
   candidato antes de filtrar, no construir el matching desde cero.
5. **Normalizar el caso ADDI antes de escalar.** El chequeo de `companyId in listOf("1", "154")` en
   `ConciliationMetricsManager.kt:337` y el comentario "will not be more" en el mismo archivo son la señal
   más clara de que hoy "borrower especial" se resuelve con un `if` puntual, no con config. Cualquier
   segundo borrower que use `PAYMENTS_VS_BORROWERS_CORE` (o que necesite agrupar su historial por mes en
   vez de por conciliación) repetirá este patrón si no se generaliza primero.
