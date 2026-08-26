# Conciliación y Distribución en `scrapy-lambdas` — JTP, Rapicredit, Solvento

> **Fecha:** 2026-08-18 · **Alcance:** repo `scrapy-lambdas` (Python/AWS Lambda), Lambda `distributions/`
> únicamente. Este documento **no** es sobre `master-servicer-apps` — ver
> [conciliacion-distribucion-estado-actual-por-borrower.md](conciliacion-distribucion-estado-actual-por-borrower.md)
> para ese repo (Inklusiva, Niko, BIA, Finkargo, Finamco, Somos, Liquitech, Vemo, Haycash, Cesionbank).
> Son dos repos distintos, con dos motores distintos, que no comparten código ni tablas.
> **Fuentes:** lectura directa de `distributions/lambda_function.py`, `main_{jtp,rapicredit,solvento}.py`,
> `distributions/src/{jtp,rapicredit,solvento,common}/*`, y `docs/solvento/distribution/payment-distribution.md`.

---

## Resumen ejecutivo

De los 9 borrowers de `scrapy-lambdas` con distribución (ver
[lambda-catalog.md](../../scrapy-lambdas/docs/business/lambda-catalog.md)... nota: solo JTP, Rapicredit y
Solvento corren dentro de la Lambda `distributions/`), el patrón se comparte a nivel de **entrypoint,
infraestructura de notificación y cliente de master-servicer**, pero **cada uno concilia contra una fuente
de verdad distinta** — no hay un "matching" único como en Inklusiva/Niko de `master-servicer-apps`. Solo
Solvento hace conciliación bancaria real (pagos contra un banco externo); JTP y Rapicredit concilian contra
sus propios archivos.

> **⚠️ JTP está desactivado.** El PR `feature/removes-jtp-distribution-terraform`
> (`distributions/terraform/main.tf`, commit `81f9aa7`) eliminó los dos schedules de EventBridge que lo
> disparaban (`jtp-1458hs` cron `58 14 ? * MON-FRI *` y `jtp-2000hs` cron `00 20 ? * MON-FRI *`) y
> reasignó ese slot horario a Solvento. Hoy **nada dispara `{"borrower": "jtp"}` automáticamente** — el
> código sigue en el repo y sigue siendo alcanzable manualmente (`main_jtp.py` / mensaje SQS manual), pero
> no corre en producción. Se documenta igual en este archivo porque el objetivo es relevar particularidades
> de diseño (útiles si JTP se reactiva o si su código se reusa/migra), **no** porque sea un patrón de
> referencia activo hoy — a diferencia de Rapicredit y Solvento, que sí corren en producción todos los días
> hábiles.

| Borrower | Conciliación contra | Umbral de tolerancia | Fuente de verdad |
|---|---|---|---|
| **JTP** | Historial propio en DB (`payment_tape`) — dedup + settlement lag por gateway | Ninguno (no hay mismatch de monto posible) | El payment tape mismo |
| **Rapicredit** | Loan tape (ownership) vía join por `id_credito` | Ninguno — % no-asignado solo se reporta | El payment tape mismo |
| **Solvento** | Banco real (Actinver, scrapeado en vivo) vs payment tape | **$1 absoluto, simétrico, todo-o-nada** | El extracto bancario |

---

## Parte 1 — Lo común

### Disparo y ruteo
Los tres llegan por la misma cola SQS a un único handler:

```
SQS (dev_distributions_standard_queue) → lambda_function.py:handler
  → body.borrower ("jtp" | "rapicredit" | "solvento")
  → jtp_master_servicer / rappi_master_servicer / solvento_master_servicer (lambda_function.py:61-165)
```

`main_jtp.py` / `main_rapicredit.py` / `main_solvento.py` solo repiten ese mismo evento SQS para correr en
local — no son entrypoints alternativos de producción.

Un único try/except en `lambda_function.handler` (`:71-79`) es la red de seguridad final: cualquier
excepción no atrapada dentro del flujo de un borrower termina posteando a `AlertsPublisher` (SNS) antes de
que la Lambda muera.

### Clases base compartidas (con adopción despareja)
| Clase base | Rol | Quién la usa de verdad |
|---|---|---|
| `PaymentDistributor` (`src/payment_distributor.py:9-40`) | ABC con `do_master_servicer`/`calculate_distribution` + helpers de Slack | Solo **Solvento** la hereda. JTP y Rapicredit duplican el mismo patrón sin heredar |
| `PaymentRetriever` (`src/payment_retriever.py:4-12`) | ABC con `retrieve_tapes(day)` | Rapicredit y Solvento la heredan; **JTP** inlinea la misma lógica dentro de `Jtp` sin clase separada |
| `DocumentsGenerator` (`src/document_generator.py:4-13`) | ABC con `generate_documents` | Rapicredit y Solvento la heredan; **JTP** usa una función suelta (`jtp/documents_generator.py`) |

Es decir: el "patrón común" existe como diseño, pero JTP es el que menos lo sigue estructuralmente aunque
comparte el mismo comportamiento externo.

### Infraestructura compartida
- **`src/py_utils/master_servicer_client.py:create_distribution`** — los tres registran la distribución
  calculada contra `POST /master-trust-servicer/master-trusts/{id}/distributions` del master-servicer-api
  (autenticado M2M). Es el sistema de registro real de movimiento de dinero, igual para los tres.
- **Config de cuentas por archivo JSON estático**: `jtp_ms.json`, `rapicredit_ms.json`, `solvento_ms.json`
  (uno por borrower, checked-in, con mapeo lender→`account_id` por ambiente dev/stg/prod). Los tres
  `generate_payload` los leen directo del disco, no vía `config.py`.
- **`src/common/repository/documents_repository.py` + `send_email_repository.py`** — los tres arman la
  URL prefirmada del documento generado y disparan el email de "instrucción de transferencia" con asunto
  propio por borrower.
- **Slack** es el canal universal de estado interno y error (mismo `slack_sdk.WebClient`, token
  `SOLVENTO_SLACK_TOKEN`, un canal interno por borrower + un canal de error compartido). **Email** se usa
  solo para el documento final de instrucción, una vez por corrida exitosa. **SNS** (`AlertsPublisher`) es
  la escalación de última instancia en cualquier excepción no controlada.
- **`src/py_utils/date_utils.py:is_business_day`** — gate de día hábil, pero con calendario **por país**:
  JTP y Rapicredit usan `'COL'`... aunque en la práctica **Rapicredit nunca invoca el chequeo** en su
  `do_master_servicer` (se importa pero no se llama) — corre cualquier día que llegue el mensaje SQS.
  Solvento usa `'MX'`.
- **Ningún rollback parcial**: los tres envuelven su flujo completo en un único try/except; si algo falla a
  mitad de camino, no hay compensación — solo se avisa por Slack/SNS y la corrida se aborta antes de
  `create_distribution`.

### Lo que **no** es común
No existe una noción compartida de "% no conciliado detiene la corrida" (como el gate del 10% de Inklusiva
en `master-servicer-apps`). Cada borrower decide solo, y de maneras muy distintas, qué hacer con lo que no
matchea — desde "solo reportar" (Rapicredit) hasta "excluir la transacción completa" (Solvento). Tampoco
hay una tabla o archivo de idempotencia común: JTP usa MySQL, Rapicredit y Solvento usan un CSV-ledger en
S3, cada uno con su propio formato de clave.

---

## Parte 2 — Particularidades por borrower

### JTP — sin conciliación bancaria; concilia contra su propio historial
- **Fuente de datos**: CSVs de payment tape en S3 (por rango de fechas, para cubrir fines de semana/
  feriados) + la tabla MySQL `payment_tape` como historial de lo ya distribuido/pendiente. Hay una clase
  `PseDisbursementsRetriever` (Google Drive) que **no se invoca** en el flujo principal — parece código
  muerto o cableado desde otro lugar no visible en este repo.
- **"Conciliación" real** = filtrado por settlement lag de gateway + dedup, no un matching monto-a-monto:
  - EFECTY solo es distribuible pasado su ciclo bisemanal (lunes/jueves).
  - PSE solo es distribuible con un día de rezago.
  - Todo lo demás liquida el mismo día.
  - Dedup contra el historial completo en `payment_tape` por `jtp_transaction_id` (o
    `payment_method+trazability_code+loan_id+transaction_date` para PSE/EFECTY) — lo descartado se sube a
    Slack como CSV, no se pierde silenciosamente.
  - **Detección de conflicto de ownership** (mismo pago con más de un `investor_name`): se detecta pero
    **no se excluye** — se reincluye en el set a distribuir. Es la particularidad más riesgosa de JTP: un
    conflicto detectado no bloquea nada.
- **Split**: suma `principal + interés del inversionista` por `investor_name`; JTP se queda con
  intereses propios + cargos + moras + sobrepagos de **todas** las filas (su fee de servicing, sin importar
  de quién es el préstamo).
- **Persistencia**: es el único de los tres que **escribe en MySQL** (`payment_tape`, upsert por
  `distribution_id`). Rapicredit y Solvento no tocan esa tabla.
- **Gate especial**: un `ValueError` conocido ("too many values to unpack") se traduce a un mensaje de
  Slack específico — "JTP no subió payment tape, no se ejecutó la distribución" — un caso de falla ya
  anticipado y mapeado a lenguaje de negocio.

### Rapicredit — concilia ownership (loan tape), no banco
- **Fuente de datos**: dos CSVs por fecha en S3 — payment tape y loan tape (ambos entregados por el
  borrower; no hay scraping de banco).
- **Matching**: `INNER JOIN` payment tape ↔ loan tape por `id_credito` — el loan tape dice de quién es
  cada crédito. Lo que no matchea (`id_credito` sin loan tape) se reporta como `amount_unassigned` /
  `amount_unassigned_percentage`, **pero no hay umbral que frene la corrida** por ese porcentaje — es
  puramente informativo.
- **Idempotencia**: clave sintética `monto+fecha+cédula+id_credito` contra un ledger CSV
  (`rapicredit/processed_payments.csv`) en S3, con snapshot antes de cada sobreescritura.
- **Filtro defensivo hardcodeado**: descarta filas con `Medio_de_pago == "siniestro"` (reclamo de seguro),
  comentado explícitamente en el código como fix contra datos malos del tape.
- **Normalización de inversionista**: variantes de texto (`"ALMAVEST I 0 a 60"`, `"ALMAVEST I 61 a 180"`,
  etc.) se colapsan en buckets canónicos (`ALMA I`, `ALMA II`, `ALMA SUNBIRD`, `IRIS`); todo lo que no
  matchea un bucket conocido cae en `RAPICREDIT` como residual — un mecanismo de fuzzy-matching que ni JTP
  ni Solvento tienen.
- **Doble partida por inversionista**: cada lender conocido recibe su asignación primaria **más** una
  espejo `SUB-ACCOUNT_<lender>` — patrón de bookkeeping único de Rapicredit.
- **Nota de deuda técnica**: hay un `# TODO: PERCENT is missing in this formula?` en el cálculo de balance
  residual de Rapicredit (`rapicredit_distributor.py:207`) sobre una función que además parece no estar
  conectada al flujo real (`add_rapicredit_balance`).
- **Sin gate de día hábil** (a diferencia de JTP y Solvento) y **sin `DistributionPayload`/tolerancia de
  monto** de ningún tipo.

### Solvento — la única con conciliación bancaria real
- **Fuente de datos**: scraping en vivo de Actinver (cuenta bancaria, 90 días acumulados siempre, no
  delta) + payment/loan tape en S3. Pagos sin `LOAN_ID` en el loan tape se descartan como huérfanos.
  Celdas con múltiples claves bancarias se explotan en filas individuales (`SplitPayments`) antes de
  matchear.
- **Matching**: agrupa por `CLAVE_RASTREO` (clave de rastreo bancaria) y compara `monto de la transacción`
  vs `suma de pagos asociados`. **Tolerancia: $1 absoluto, simétrico.** Si no hay pago asociado, o la
  diferencia excede $1, **toda la transacción se excluye** de la corrida (no hay aplicación parcial).
- **Manejo de inconsistencias**: las transacciones excluidas no se marcan como procesadas — reintentan
  automáticamente al día siguiente una vez que la data corriente se corrija. El monto de las transacciones
  inconsistentes se resta del residual de Solvento (queda sin asignar mientras se corrige, no se le
  acredita a nadie).
- **Split**: `Lendable` y `BBVA` reciben su monto matcheado (capado para no exceder el depósito real);
  Solvento se queda con `saldo − Lendable − BBVA − inconsistentes − en tránsito − reserva ad hoc`.
- **Gate de día hábil** contra calendario **mexicano** (única de las tres con calendario `MX`).
- **Persistencia**: 100% S3 (ledger `processed_transactions.csv`, `payment_summary.csv`,
  `transactions_with_inconsistent_payments.csv`) — sin escritura en MySQL.

---

## Parte 3 — Tabla comparativa

| Aspecto | JTP | Rapicredit | Solvento |
|---|---|---|---|
| Calendario de día hábil | COL | *(no se aplica en el código)* | MX |
| Guard de re-corrida diaria | Dedup por historial en `payment_tape` (MySQL) | `s3.exist_folder("rapicredit/{day}")` | `s3.exist_folder("solvento/{day}")` |
| Contraparte de conciliación | Ninguna externa — dedup + settlement lag | Loan tape (ownership) | Banco real (Actinver) |
| Llave de match | `jtp_transaction_id` / `(método, trazabilidad, loan_id, fecha)` | `id_credito` | `CLAVE_RASTREO` |
| Tolerancia de monto | N/A | N/A (solo se reporta el % no asignado) | **$1 absoluto, todo-o-nada** |
| Ledger de idempotencia | Tabla `payment_tape` (MySQL) | `rapicredit/processed_payments.csv` (S3) | `solvento/processed_transactions.csv` (S3) |
| Fórmula de split | Capital+interés por inversionista; JTP se queda fees/mora/sobrepago | Suma `Monto_pagado` por inversionista normalizado | Depósito − Lendable − BBVA − inconsistentes − en tránsito − reserva |
| ¿Quién es la fuente de verdad? | El tape mismo | El tape mismo | El extracto bancario |
| Escribe en MySQL | Sí (`payment_tape`) | No | No |
| Fuente externa además de S3/DB | Google Drive (PSE, aparentemente sin uso real) | Ninguna | Scraping en vivo de Actinver |
| Documentos por email | 2 | Hasta 12 (anexos de cumplimiento) | 1 |
| Particularidad más riesgosa | Conflictos de ownership detectados pero **no excluidos** | Balance residual con fórmula incompleta (`TODO` sobre `PERCENT`); sin gate de día hábil | Ninguna — es la implementación más estricta de las tres |

---

## Notas

- Los tres comparten **infraestructura de notificación y el cliente de master-servicer**, pero **no
  comparten lógica de conciliación entre sí** — cada uno resuelve "qué es un pago válido para distribuir"
  de forma completamente distinta. No hay, hoy, un módulo genérico de conciliación reusable dentro de
  `distributions/` como sí existe (parcialmente) en `master-servicer-apps`.
- El documento `docs/solvento/distribution/payment-distribution.md` (en `scrapy-lambdas`) ya documenta el
  detalle de Solvento con precisión; este documento lo resume en el contexto comparativo de los tres
  borrowers de la Lambda `distributions/`.
- Riesgos a seguir de cerca si se migra alguno de estos tres al motor config-driven de
  `master-trust-servicer-api`: el re-include de conflictos de ownership en JTP, el gate de día hábil
  ausente en Rapicredit, y la fórmula de balance con `TODO` pendiente en Rapicredit.

---

## Parte 4 — Qué le pide cada particularidad a un motor genérico

Si el objetivo es diseñar un motor de conciliación/distribución genérico (config-driven, en la línea de
`distri-engine`), lo que sale de comparar estos tres borrowers **no es un catálogo de features exóticas**
— son variaciones sobre 4 preguntas que el motor tendría que responder por config, no por código:

### 1. ¿Contra qué se concilia? (fuente de verdad)
Los tres responden distinto: JTP contra su propio historial (dedup + lag), Rapicredit contra un segundo
archivo del propio cliente (loan tape / ownership), Solvento contra un tercero externo (banco). Un motor
genérico necesita que la "fuente de verdad" sea un tipo de conector configurable, no un si/no binario de
"¿hay banco o no?" — porque acá ya hay tres modos distintos y ninguno es el "raro": son igual de válidos.

### 2. ¿Qué tan estricta es la tolerancia?
Solo Solvento tiene un umbral de monto (**$1 absoluto, todo-o-nada**, sin aplicación parcial). Rapicredit
mide un % no-asignado pero **no lo usa para frenar nada** — es un caso intermedio que un motor genérico
debería soportar explícitamente (medir sin bloquear), no solo el binario "bloquea / no bloquea" que sí
existe en `master-servicer-apps` (Inklusiva). Es una tercera opción real, no un descuido de Rapicredit.

### 3. ¿Qué pasa con lo que no concilia?
Tres comportamientos distintos, los tres deliberados:
- **JTP**: lo detectado como conflicto se **reincluye** (posible doble conteo — este es el único caso de
  los tres que parece un bug más que un diseño, y valdría la pena confirmarlo con el equipo antes de
  tomarlo como "particularidad a soportar").
- **Rapicredit**: lo no-asignado se **reporta y sigue** (nunca bloquea, nunca se excluye del todo).
- **Solvento**: lo inconsistente se **excluye por completo** y reintenta solo, al no marcarse como
  procesado — un motor genérico necesitaría este concepto de "excluir sin marcar como fallido
  permanente" para soportar el mismo reintento automático.

### 4. ¿Cómo se calcula el split final?
- JTP y Rapicredit: suma directa por dueño sobre filas ya etiquetadas.
- Solvento: el owner "residual" (Solvento mismo) se calcula por **resta contra un saldo**, no por suma —
  el mismo patrón `poolSource: ACCOUNT_BALANCE` que ya aparece en Somos dentro de `master-servicer-apps`
  (ver [conciliacion-distribucion-estado-actual-por-borrower.md](conciliacion-distribucion-estado-actual-por-borrower.md),
  sección Somos, y **E3** de [epica-distri-engine.md](epica-distri-engine.md)). Es la segunda evidencia
  independiente, en un repo distinto, del mismo requisito — refuerza que `poolSource` configurable no es
  un caso aislado de un solo deal.

### Particularidades que probablemente NO valga la pena generalizar
- El fuzzy-bucketing de nombres de inversionista de Rapicredit (`"ALMAVEST I 0 a 60"` → `ALMA I`) y la
  doble partida `SUB-ACCOUNT_<lender>` son específicos de cómo ese cliente nombra sus cuentas — mejor
  resolverlos como mapeo de datos en la config del deal, no como lógica nueva del motor.
- El explode de celdas multi-clave de Solvento (`SplitPayments`) es un parseo de formato de origen, no una
  regla de negocio — pertenece a la capa de ingesta/normalización, no al motor de conciliación.

### Estado de JTP frente a esta apuesta
Como JTP está desactivado (ver nota al inicio), sus particularidades **no deberían pesar** en el diseño del
motor genérico salvo la de reincluir conflictos de ownership — y esa, justamente, es la que primero habría
que confirmar si es un bug o una decisión de negocio antes de decidir si el motor genérico la soporta o la
prohíbe por default.
