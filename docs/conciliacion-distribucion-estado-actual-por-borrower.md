# Conciliación y Distribución en `master-servicer-apps` — lo común y las particularidades por borrower

> **Fecha:** 2026-08-18 · **Alcance:** solo el repo `master-servicer-apps` (Python/Scrappy). El motor
> config-driven de `master-trust-servicer-api` (Kotlin) está fuera de este documento — para eso ver
> [epica-distri-engine.md](epica-distri-engine.md).
> **Fuentes internas verificadas contra código:** `docs/conciliation_and_distribution_explained.md`,
> `docs/business/conciliation-workflow.md`, `docs/business/distribution-workflow.md`,
> `docs/conciliation/borrowers-conciliation-keys.md`, y lectura directa de cada `task.py` por borrower.

---

## Resumen ejecutivo

El "patrón común" (Inklusiva/Niko: llave de gateway → conciliación con umbral 10% → ownership API →
split binario owner/borrower → `DistributionPayload`/`Assignment` → cliente de master servicer → email
con adjuntos) **es la excepción, no la regla**. De los 9 borrowers con carpeta `distribution/` en este
repo, solo 2 (Inklusiva, Niko) lo siguen tal cual. El resto reinventa partes del pipeline, lo bypasea por
completo, o nunca llegó a implementarse.

| Borrower | Conciliación aquí | Distribución aquí | Estado |
|---|---|---|---|
| **Inklusiva** | Sí — Python/Pandas por gateway | Sí — patrón de referencia | ✅ Completo, es el modelo |
| **Niko** | Sí — dual (Stripe + banco directo) | Sí — sigue el patrón | ✅ Completo |
| **BIA** | ❌ No existe | Sí — pero atípica en casi todo | ⚠️ Split propio, sin `Assignment` |
| **Finkargo Colombia** | Sí — solo PT↔FondoTransferencia, sin `Payment` | Sí — atípica | ⚠️ Multi-moneda y ownership-check ya construidos |
| **Finamco** | Sí — con validación externa RADIAN | Sí — bypasea el motor genérico | ⚠️ POST directo a un endpoint custom |
| **Somos** | Sí — vía JOIN SQL genérico | Sí — pero por `entrypoint` directo, no BPM | ⚠️ Arquitectura distinta (OOP, no task) |
| **Liquitech** | Placeholder vacío (`# Do things!`) | Script manual estilo notebook | 🔴 El outlier — no es ni BPM ni automático |
| **Vemo** | Sí (conciliación + carga de ownership a Atom) | ❌ Borrada el 2026-05-29 | 🔴 Sin distribución en este repo hoy |
| **Haycash** | ❌ No existe | Scaffolding nunca implementado | 🔴 `task.py` es un template vacío |
| **Cesionbank** | ❌ No existe | No es distribución — es un scraper de Selenium | 🔴 Carpeta mal etiquetada, clonada de BIA |
| Exitus, Coograncolombiana, Equity Link, Generandorlq, Hilco Arr. | Sí (Python/Pandas) | ❌ No existe aquí | Solo escriben las FKs; la distribución corre en `master-trust-servicer-api` |
| ADDI, ADDI_BNPN, PayJoy, Sistecredito, Welli | Genérico (JOIN SQL / core), sin carpeta scrappy | ❌ No existe aquí | Viven enteramente fuera de este repo |

---

## Parte 1 — Los tres mundos: dónde vive cada borrower

No todos los borrowers usan `master-servicer-apps` de la misma manera. Hay tres arquitecturas distintas
conviviendo:

### A. Pipeline legacy completo en este repo (conciliación *y* distribución, código Python a medida)
Inklusiva, Niko, BIA, Finkargo Colombia, Finamco, Liquitech (roto), Haycash (nunca implementado),
Cesionbank (mal etiquetado). Estos son los "deals scrappy" que la épica de distri-engine quiere migrar
hacia el motor config-driven.

### B. Solo conciliación aquí — la distribución ya vive en `master-trust-servicer-api`
Exitus, Coograncolombiana, Equity Link, Generandorlq, Hilco Arrendamiento Productivo: tienen
`conciliation/conciliation_calculation/` pero **ningún** `distribution/` en este repo. El scrappy de
conciliación escribe las FKs (`payment_id` en `payment_tape`) que el motor de `master-trust-servicer-api`
lee para distribuir. Esto es exactamente la frontera que describe **E8** del épica ("la conciliación de
plataforma solo reporta; el matching real es el extractor / Conci Engine del backoffice, que recién
nace") — para estos 5 borrowers, el "extractor" ya es este repo.

### C. Ni conciliación ni distribución aquí — viven enteramente en la plataforma/config
ADDI, ADDI_BNPN, PayJoy, Sistecredito, Welli: **no tienen carpeta `python_apps/scrappy/{borrower}/`**.
Su conciliación corre por el JOIN SQL genérico (`core/conciliation/payment_vs_bank`,
`storage_conciliations.conciliate_payment_vs_payment_tape()`) disparado desde otro lado (no BPM de este
repo), y su distribución es 100% `master-trust-servicer-api`.

### D. Arquitectura propia, ni BPM ni scrappy — Somos
Somos corre por `entrypoints/distribution/main.py` (un script directo, no SQS/BPM) con una clase
`Distributor` (OOP, no `task.py`) que internamente llama al mismo JOIN SQL genérico de la categoría C
para su propia conciliación. Es el único borrower que mezcla "conciliación genérica" con "distribución
completa en este repo", pero sin pasar por el mecanismo Scrappy de ningún otro borrower de la categoría A.

---

## Parte 2 — El patrón común (cuando existe)

Esto es lo que Inklusiva y Niko implementan al pie de la letra, y que el resto de la categoría A
reutiliza solo parcialmente.

### Stage 0 — File parsing
Cada gateway deposita un archivo crudo (xlsx/csv) en un prefijo S3 propio del cliente. Una tarea
`S3ParsingTask` (`file_parsing/task.py`) lista los objetos recientes, los normaliza y reinyecta las filas
en la tabla `payment_tape` vía el pipeline de ingesta.

### Stage 1 — Conciliación
Dos formas de matchear coexisten en el repo:

| | JOIN SQL genérico | Predicado Python/Pandas por gateway |
|---|---|---|
| **Dónde vive** | `core/conciliation/*`, `storage_conciliations.conciliate_payment_vs_payment_tape()` | `{borrower}/conciliation/conciliation_calculation/task.py` |
| **Cómo matchea** | Un solo `INNER JOIN` en MySQL sobre columnas virtuales `v_normalized_gateway_payment_id` / `v_normalized_provider_id` (case/guiones normalizados), en lotes de 100, con `FORCE INDEX` | Trae Payments + PT a memoria como DataFrames, arma una llave distinta por gateway (a veces con tolerancia de monto) |
| **Quién lo usa** | ADDI, ADDI_BNPN, PayJoy, Sistecredito, Somos (y Welli para parte de sus gateways) | Inklusiva, Niko, Exitus, Coograncolombiana, Equity Link, Vemo, Welli (Wompi/Bancolombia Correspondent) |
| **Tolerancia** | Match exacto (normalizado), sin fuzzy amount | Depende del borrower — `< 0.10`/`< 1` típico |

Tres tipos de conciliación conviven:
1. **`PAYMENTS___VS___PAYMENT_TAPE`** — hay capa `Payment` (gateway-mediada). Inklusiva, Niko-Stripe.
2. **`PAYMENT_TAPE___VS___BANK`** — no hay `Payment`; el PT se matchea directo contra `FundTransfer`.
   Niko-banco-directo, Finkargo Colombia, Finamco.
3. **`PAYMENTS___VS___BORROWER_DB`** — contra la BD interna del borrower. Solo ADDI.

**Safety gate:** cuando existe (Inklusiva, Niko, Finkargo Colombia), si el % no-conciliado supera un
umbral (típicamente 10%, por conteo o por monto según el borrower) la corrida se frena con una excepción
y una alerta — nada se persiste. **No es universal**: Finamco no tiene este gate y siempre continúa
("fail-open"); BIA no tiene conciliación de ningún tipo.

### Stage 2 — Distribución
1. Elegibilidad: filas de PT `reconciled=True, distributed=False` (`get_not_distributed_df`).
2. Ownership: consulta a la Ownership/Atom API para saber a qué inversionista está cedido cada contrato.
3. Split: fórmula de reparto entre las partes (owner vs. borrower, u otros).
4. Chequeo de integridad: `suma(partes asignadas) == monto_total`, con tolerancia chica; si falla, se
   excluye la fila o se frena la corrida entera (varía por borrower).
5. Persistencia: un `DistributionPayload` con `Assignment`s se envía al `MasterServicerClient`, que
   ejecuta las transferencias reales; cada fila de PT queda estampada con `distribution_id`.
6. Notificaciones: email con Excel/PDF adjuntos a las partes relevantes, y/o alerta interna (Roam).

**Esto tampoco es universal.** BIA, Finamco y Liquitech bypasean por completo el
`DistributionPayload`/`Assignment`/`MasterServicerClient` — dos hacen `UPDATE`/POST directos, uno arma el
payload a mano y lo manda a un endpoint hardcodeado.

### Mecanismos de ejecución
- **Vía BPM (Scrappy):** `BPM → SQS → bpm_task_trigger_queue_consumer → EntryPoint → ScrappyManager →
  {Conciliation,Distribution}Task.job()`. El mecanismo de casi todos los borrowers de categoría A.
- **Vía entrypoint directo:** `entrypoints/distribution/main.py` — usado solo por Somos hoy (la única
  implementación real de `strategist.get_distributor_impl`).
- **Vía Scrappy H2H (SFTP):** `entrypoints/scrappy/h2h/` — envío de instrucciones cifradas (GPG), fuera
  del alcance de conciliación/distribución propiamente.

### Módulos core compartidos
| Módulo | Rol |
|---|---|
| `core/distribution/distributor.py` | Orquestador base (interfaz `Distributor`, solo Somos la implementa hoy) |
| `core/distribution/strategist.py` | Selección de implementación por borrower |
| `core/distribution/somos/*` | Todo el detalle de Somos: ownership, subscripciones, scheduler, documentos |
| `core/conciliation/payment_vs_bank/`, `payment_vs_borrowers_core/`, `payment_vs_payment_tape/` | Conciliadores genéricos (JOIN SQL) |
| `internal/storages_services/database/payments/storage_conciliations.py` | El JOIN SQL genérico |

---

## Parte 3 — Particularidades por borrower

### Inklusiva — el modelo de referencia
Ya documentado en detalle en `docs/conciliation_and_distribution_explained.md`. Llave de match distinta
por gateway (EFECTY/PSE/WOMPI/Bancolombia Correspondent), corte 10% por **conteo**, split binario
100%-owner / 100%-Inklusiva, dos bancos liquidadores (Bancolombia / Banco de Bogotá), y un caso especial
de recuperación (**barrido**, `barrido_pagos_viejos/pt_by_vaas`) que sintetiza filas de PT para pagos
aprobados que nunca llegaron por archivo bancario.

### Niko — conciliación sin gateway
Dos caminos conviven: **Stripe** (Payment → Disbursement → FundTransfer, tres capas a verificar) y
**BBVA/ACTINVER** (transferencia bancaria directa, sin `Payment` en VAAS — el ancla de match es
fecha+monto+cuenta, no un `provider_id`). El corte 10% se calcula sobre **monto**, no conteo (para no
dejar pasar una transferencia grande individual).

### BIA — sin conciliación, split por componente de factura
- **No existe conciliación** para BIA en este repo — pasa directo a distribución sobre un `payment_tape`
  poblado por otra vía (export del sistema de facturación).
- Distribución lee `bills.csv`/`bill_details.csv`/`anticipos.csv` de S3 (no solo DB), con un gate de día
  hábil y un hard-fail si algún pago no-anticipo tiene más de 30 días (`task.py:229-235`) — un guardrail
  de frescura que ningún otro borrower tiene.
  Split **no es binario**: cada factura tiene un peso por tipo de componente (~50-100 "kinds" de facturación
  de servicios, hardcodeados por banco en `componentes_BBVA`/`componentes_SANTANDER`,
  `SQL_functions_bia.py:52-125`), más un ajuste de compensación cuando pagos parciales/créditos distorsionan
  el split ponderado. Los anticipos corren en un sub-flujo separado, 100% a un lado u otro según
  `cedido`.
- **Sin `DistributionPayload`/`Assignment`**: marca las filas vía `UPDATE payment_tape` directo
  (`marcar_distribuidos`, `SQL_functions_bia.py:148-221`, `distribution_id` siempre `None`) y sube un CSV
  a S3 para que un proceso externo/manual ejecute el pago. Notificación solo por Roam, sin email a
  terceros.

### Finkargo Colombia — sin capa `Payment`, ya resuelve lo que la épica propone
- Conciliación es PT↔FondoTransferencia puro (nunca toca `payments`), con dos caminos paralelos: **Supra**
  (multi-día, "en tránsito", agrupación por fecha/cuenta/monto) y **banco** (referencia + tolerancia).
  Corte 10% por monto, sin override real.
- **Ya implementa el cross-check de ownership que la épica propone construir en E1**: si el owner del
  tape no coincide con el de la Ownership API, distribuye al de la API y deja un rastro en
  `status_reason` (`task.py:531, 819-824`) — sin estrategia de bloqueo, siempre "API gana".
- **Multi-moneda (USD/COP) ya soportado por fila** — precisamente lo que la épica **E9** describe como
  ausente en `master-trust-servicer-api`.
- El split no es owner-binario: hay un fee/spread que absorbe Finkargo (nunca los lenders), y **dos
  distribuciones por corrida** (una por "cuenta", otra por "tránsito"), cada una con su propio
  `DistributionPayload`.

### Finamco — validación externa RADIAN + POST directo, sin gate de 10%
- Conciliación es PT↔FondoTransferencia (sin `Payment`), pero con una capa extra única: sube CUFEs a un
  endpoint del backoffice, sondea hasta 30 minutos un reporte RADIAN (tercero fiscal), y cruza
  owner/monto contra él. **Si RADIAN falla, el chequeo se marca "pasa" igual** (fail-open) — nunca
  bloquea la conciliación.
- **No tiene el gate de 10%**: siempre continúa y solo reporta el desglose de razones.
- El owner sale de una columna de texto libre en el PT (`extra_data.aux_var_3`), normalizada por un
  diccionario de alias mantenido a mano; un owner no mapeado **frena toda la corrida** (a diferencia de
  Inklusiva, que particiona los "ownerless" y sigue).
- **Bypasea el motor genérico por completo**: arma dos "planos" (JSON) y los POSTea directo a un
  endpoint HTTP hardcodeado — nunca instancia `DistributionPayload`/`Assignment`/`MasterServicerClient`.

### Somos — arquitectura OOP, pool = saldo de cuenta
- No corre por BPM/Scrappy: un `entrypoint` directo instancia `SomosDistributor`, la única implementación
  real de la interfaz `Distributor`. Corre en fechas configurables por calendario mensual
  (`RunConfig`/`MonthDate`), no "cada vez que hay pagos".
- Ownership tiene un fallback único: si un contrato no tiene owner propio, sube por la cadena de
  renovaciones de suscripción (`SubscriptionManager`) hasta encontrar el de un contrato ancestro.
- Conciliación vía el JOIN SQL genérico (igual que ADDI/PayJoy/Sistecredito).
- **El monto a distribuir para el owner Somos ya es el saldo de cuenta, no la suma de pagos**
  (`saldo_cuenta − distribuciones_a_lenders − pagos_no_conciliados`) — exactamente el patrón que la
  épica quiere generalizar como `poolSource: ACCOUNT_BALANCE` en **E3**.

### Liquitech — el outlier real
- La conciliación (`pre_conciliation`) es un shell vacío, nunca implementado (mismo scaffolding
  `# Do things!` que Haycash).
- La distribución **no es una tarea BPM**: es un script (`distribution.py`) pensado para correrse
  celda-por-celda por un operador, con comentarios literales tipo
  `# ------ CORRER DESDE AQUÍ ------` y `# OJO: PUNTO DE NO RETORNO`.
- Datos hardcodeados en código: cuentas/NIT de 13 inversionistas (`skandia_integration.py:15-46`),
  listas de override manual editadas a mano antes de cada corrida, un webhook n8n sin auth para
  verificación RADIAN.
- Python calcula la instrucción completa (yields de inversionista, splits) y la **POSTea directamente**
  al endpoint de distribución de `master-trust-servicer-api` — arquitectura invertida respecto al modelo
  que la épica asume (donde master-trust calcula el pool/assignment). Esto confirma el diagnóstico de
  **E7** del épica: reemplazar este camino es "borrar antes que construir".

### Vemo — la distribución desapareció del repo
`distribution/distribution_calculation/` (task.py + test.py + utilities.py, ~500 líneas) fue borrada el
2026-05-29 en el commit `08bd9ff` ("Feature/vemo auto ownership update #236). El alcance declarado de ese
PR era solo el formato de subida del delta de ownership a Atom (`specs/1780000986_vemo_ownership_atom_loader_upload`)
— la eliminación del código de distribución no está explicada en la story/spec. Hoy Vemo solo tiene
conciliación (`conciliation_calculation` + `file_parsing`, incluyendo el export de ownership al bucket de
Atom). **Sin verificar directamente, pero consistente con el resto del repo**: lo más probable es que la
distribución de Vemo corra hoy enteramente en `master-trust-servicer-api`, con este repo alimentándolo
solo de conciliación y datos de ownership.

### Haycash — nunca implementado
`distribution/distribution_calculation/task.py` es el scaffolding genérico de Scrappy sin llenar: rango
de fechas hardcodeado a un mes de 2025, un comentario literal `# Do things!` donde debería ir la lógica,
y un `Output` con `required_output_field="test"`. No hay carpeta de conciliación en absoluto. No sirve
como referencia de nada — solo como ejemplo de "task registrado pero nunca construido".

### Cesionbank — mal etiquetado, es un scraper
La carpeta `distribution/downloaddata/` no distribuye nada: es un scraper de Selenium que hace login en
un portal de Sistecredito y descarga CSVs. Está clonado de BIA (importa `Input`/`Output` del modelo de
distribución de BIA, y el registro de BPM lo declara como `process: BIA_DISTRIBUTION`). Tiene además un
bug de runtime: llama funciones definidas como métodos de instancia (`self`) como si fueran funciones de
módulo. No hay conciliación para Cesionbank en este repo.

---

## Parte 4 — Qué tan "común" es el patrón común, en la práctica

| Borrower | ¿Capa `Payment`? | Ownership vía API | Split | Gate 10% | `DistributionPayload`/`Assignment` |
|---|---|---|---|---|---|
| Inklusiva | Sí | Sí | Binario owner/borrower | Sí (conteo) | Sí |
| Niko | Sí (Stripe) / No (banco) | N/A | N/A (solo concilia) | Sí (monto) | N/A |
| BIA | No | Sí (Atom) | Ponderado por componente | No (sin conciliación) | **No** — `UPDATE` directo |
| Finkargo Colombia | No | Sí (con cross-check) | Fee/spread + binario | Sí (monto, sin override) | Sí |
| Finamco | No | No (columna de texto) | Binario por owner | **No** (fail-open) | **No** — POST directo |
| Somos | Sí (genérico) | Sí (+ cadena de suscripción) | Saldo de cuenta | N/A (JOIN exacto) | Sí (interno al `Distributor`) |
| Liquitech | No | Manual (RADIAN) | Yield de inversionista | N/A (script manual) | **No** — POST directo desde Python |
| Vemo | Sí (solo concilia) | — | — | — | — (sin distribución aquí) |
| Haycash | — | — | — | — | — (nunca implementado) |
| Cesionbank | — | — | — | — | — (no es distribución) |

---

## Parte 5 — Notas para la épica de distri-engine

- **E1 (validación de ownership):** Finkargo Colombia ya implementa el cross-check tape-vs-API con
  estrategia "API gana" — es un precedente directo, no un diseño desde cero.
- **E3 (pool configurable):** Somos ya calcula el monto a distribuir del owner Somos como saldo de cuenta,
  no como suma de pagos — mismo concepto que `poolSource: ACCOUNT_BALANCE`.
- **E7 (instrucción multi-canal / matar el adapter de Liquitech):** confirmado desde el lado Python — la
  arquitectura actual invierte quién calcula la distribución (Python arma el payload y lo empuja a
  master-trust), consistente con el diagnóstico del épica sobre `PostDistribution.kt`.
- **E8 (conciliación como gate):** los 5 borrowers de la categoría B (Exitus, Coograncolombiana, Equity
  Link, Generandorlq, Hilco Arr.) son exactamente los deals donde el matching **ya** corre en este repo y
  escribe FKs en plataforma — son los candidatos naturales para estrenar el gate, sin esperar migración de
  matching.
- **E9 (multi-moneda):** Finkargo Colombia ya soporta dos monedas por regla en este repo — vale la pena
  revisar su implementación como referencia antes de construir la de `master-trust-servicer-api` desde
  cero.
- **Riesgo no cubierto por ningún ticket actual:** BIA, Finamco y Liquitech distribuyen dinero real hoy
  sin pasar por `DistributionPayload`/`Assignment`/`MasterServicerClient` — cualquier gate o validación
  que la épica agregue ahí (E1, E2, E8, readiness checks) **no los va a tocar** hasta que esos tres
  bypasses se retiren.
