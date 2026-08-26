# El proceso de distribución — paso a paso real, y hacia un pipeline unificado

> **Fecha:** 2026-08-20 · **Alcance:** el proceso de EJECUCIÓN de una distribución (qué pasa cuando corre),
> complementario a `docs/mapeo-borrowers-configuracion.md` (que cubre la CONFIGURACIÓN — el wizard).
> **Fuente principal:** código real de `master-trust-servicer-api` — `DistributionRunner.kt`,
> `DistributorAdapter.kt`, `DistributorCalculator.kt`, `DistributionCreator.kt` (leídos completos el
> 2026-08-20, no parafraseados de la épica). Cruzado contra `conciliacion-distribucion-estado-actual-por-borrower.md`
> y `conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md` para las variantes Python/Lambda.

---

## 1 — El pipeline real hoy, paso a paso (master-trust-servicer-api, Kotlin)

Verificado clase por clase, en orden real de ejecución. Cada paso indica con qué etapa del wizard
(`docs/mapeo-borrowers-configuracion.md`) se corresponde su configuración.

| # | Paso | Clase real | Config que lo alimenta (wizard) |
|---|---|---|---|
| 1 | **Trigger** — tarea BPM (`{borrower}_DISTRIBUTION`) o llamada manual `distribute(date, borrower)` | `DistributionRunner.distribute()` | — |
| 2 | **Resolver config** — carga `DistributionConfig` completo desde `distribution_config.config_json` + su `MasterTrustServicer` | `BorrowerConfigResolver.getByBorrowerCode()` | Deal Info (VPR-9644) |
| 3 | **Readiness check** (el único real hoy: frecuencia) | `DistributionFrequencyChecker.canDistribute()` | Readiness Checks (VPR-9637) — hoy solo 1 de los 3 checks transversales que la épica propone |
| 4a | **Calcular ventana de fechas** — ajusta `daysBackToRetrievePaymentTapes` por días hábiles del país del borrower | `DefaultDistributablePaymentsFetcher` (usa `DateHelper.getWorkingDaysBack`) | Pool Strategy → Days back (VPR-9628) |
| 4b | **Query de payment tapes candidatos** — por `companyId`, rango de fechas, `distributed=false`, + 3 flags binarios de conciliación (`withPaymentId`, `withPaymentConciliated`, `withFundTransferId`) | `PaymentTapeDataProvider.getPaymentTapes()` | Conciliation Requirements (VPR-9633) — **confirmado: es exactamente el filtro binario silencioso que E8 quiere reemplazar por un gate con tolerancia** |
| 4c | **Aplicar payment filters** (accounting payments, gateways, fechas por gateway) | `DistributionFiltersResolver.resolve()` | Accounting Payments (VPR-9631), Gateway Filters (VPR-9632), Date & Time Filters (VPR-9634) |
| 5a | **Resolver owner por payment tape** | `OwnerNameResolverProvider.getResolverForConfig()` | Ownership: Source (VPR-9635) — **confirmado: corre acá, antes de cualquier cross-check; el cross-check de VPR-9636 no existe en este paso hoy** |
| 5b | **Calcular `netAmount`** — usa `pt.netAmount` si existe; si no, `totalPayment - feeAmount`; si no, `totalPayment` crudo | `DefaultDistributionParametersCalculator.getNetAmount()` | Pool Strategy → Amount field (VPR-9628) — **este fallback de 3 niveles es el precedente real de lo que `amountField`/`custom_field` debería generalizar** |
| 6 | **Particionar ownerless vs. distribuibles** — `ownerName !in [UNKNOWN, UNDEFINED]` | `DistributorCalculator` (línea del `!!` que puede tirar NPE, ver E1) | Ownership (mismatch/fallback — VPR-9635/9636) |
| 7 | **Construir assignments** — por cada `AssignmentConfig`: matchear criteria, sumar `netAmount` al pool, aplicar `reserveAmount` (fee), correr balance check si aplica, armar tiers LENDER/BORROWER/REST | `AssignmentsResolver.calculateAssignments()` | Distribution Rules (VPR-9643) |
| 8 | **Ensamblar el resultado** — `CALCULATED` (con assignments) o `NOTHING_DISTRIBUTABLE` (si el pool quedó vacío) | `DistributionCalculator.calculate()` | — |
| 9 | **Notificar ownerless** (si hay) | `DistributionNotificationManager.notifyOwnerlessPaymentTapes()` | Notifications: Events (VPR-9639) |
| 10a | **Si CALCULATED:** construir metadata (variables para templates) | `DistributorAdapter.buildMetadata()` | Notifications: Templates — placeholders (VPR-9640) |
| 10b | **Persistir la distribución** — crea `Distribution` + `Assignment` reales (el movimiento de plata) | `CreateDistribution.apply()` | — (esto ES la ejecución) |
| 10c | **Marcar payment tapes como distribuidos** — estampa `distributionId`, guarda | `PaymentTapeDataProvider.save()` | — |
| 10d | **Generar reporte distribuido/no-distribuido** (Excel 3 tabs) | `DistributedAndUndistributedReporter.produceReport()` | Notifications: Templates (VPR-9640) |
| 10e | **Enviar instrucción de transferencia** (hoy solo email) | `DistributionNotificationManager.notifyTransferInstruction()` | Notifications: Events & Channels (VPR-9639) — E7 |
| 11 | **Notificar resultado general** (éxito/fin, corre siempre) | `DistributionNotificationManager.notifyDistributionResult()` | Notifications: Events (VPR-9639) |
| 12 | **Si algo tira excepción en cualquier punto de 4-10:** loguear, notificar error al borrower, **alerta interna separada** (Slack/ops), status=error, se relanza como `DistributionException` hacia el framework BPM | `applyDistributionCalculation()` (try/catch envolvente) + `DistributionAlertsManager.sendAlertSafely()` | Readiness Checks: failure behavior (VPR-9638) — parcialmente; la alerta interna de ops no tiene lugar hoy en ninguna historia |

### Variante Draft (revisión antes de ejecutar)

Existe un camino paralelo, no cubierto por ninguna historia del wizard: `createDraft()` corre los pasos
1-8 igual, pero en vez de persistir, guarda un `DraftDistribution` para revisión humana. Más tarde,
`distributeFromDraft()` **revalida que ninguno de esos payment tapes ya se haya distribuido por otro
lado** (lock check) y recién ahí ejecuta los pasos 10b-11 con los assignments ya fijados del draft (sin
recalcular pool/ownership). Esto es relevante para **Review & Activate (VPR-9641)** si en algún momento
se quiere ofrecer un preview/dry-run antes de activar — el mecanismo de draft **ya existe**, no habría
que construirlo de cero.

---

## 2 — Gaps: lo que el runtime real hace y que ningún ticket del wizard cubre todavía

1. **La alerta interna de ops (Slack/SNS) es un canal separado de la notificación al borrower**
   (`DistributionAlertsManager`, paso 12) — Notifications (VPR-9639/9640) diseña el canal hacia el
   cliente/lender, pero no hay historia que decida si esta alerta interna se configura por deal o es fija
   a nivel plataforma.
2. **El ajuste de "días hábiles por país" al calcular la ventana de búsqueda** (paso 4a) no aparece
   explícito en ninguna historia — está implícito en "Days back" de VPR-9628, pero el mecanismo real
   (`getWorkingDaysBack`) depende del país del borrower (Deal Info, VPR-9644), cruzando dos etapas.
3. **El flujo de Draft/Review** (sección anterior) es un mecanismo real y ya construido que **VPR-9641
   (Review & Activate)** podría reutilizar para el dry-run que dejamos como pregunta abierta ahí — no es
   necesario diseñarlo desde cero.
4. **El fallback de 3 niveles para `netAmount`** (paso 5b: `netAmount` → `totalPayment - feeAmount` →
   `totalPayment`) es el precedente real y concreto de qué debería hacer `amountField` cuando la columna
   elegida viene null — hoy VPR-9628 no especifica ese comportamiento.

---

## 3 — Comparación con los otros dos mundos (Python / Lambda)

Los mismos 12 pasos, mapeados contra los flujos ya relevados en
`conciliacion-distribucion-estado-actual-por-borrower.md` y
`conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md`:

| Paso del pipeline Kotlin | Inklusiva/Niko (Python, patrón de referencia) | JTP/Rapicredit/Solvento (Lambda) |
|---|---|---|
| 3. Readiness (frecuencia) | No explícito — corre por trigger BPM/SQS | Gate de día hábil (JTP, Solvento) — **Rapicredit no lo tiene, confirmado bug** |
| 4b. Filtro de conciliación | **Acá sí bloquea**: gate de tolerancia 10% (por conteo o monto) — lo que E8 quiere traer al motor Kotlin | Solvento: $1 absoluto todo-o-nada: Rapicredit: mide sin bloquear; JTP: dedup + settlement lag, no es un match monto-a-monto |
| 5a. Ownership | Vía Ownership API (Inklusiva/Niko) o cross-check (Finkargo) | N/A (JTP/Rapicredit resuelven por su propio tape/loan-tape; Solvento no tiene ownership, tiene split por resta) |
| 7. Assignments | Split binario owner/borrower (Inklusiva), fee/spread (Finkargo), saldo de cuenta (Somos) | Capital+interés por inversionista (JTP), suma normalizada (Rapicredit), resta contra saldo (Solvento) |
| 10b. Persistir | `DistributionPayload`/`Assignment` vía `MasterServicerClient` — **mismo contrato que el motor Kotlin busca ser el único punto de registro** | `create_distribution` vía API — mismo contrato |
| 10e. Instrucción | Email con Excel/PDF + alerta Roam | Email (1-12 documentos) + Slack interno + SNS en excepción no controlada |

**La conclusión que importa para el pipeline unificado:** los 12 pasos son los mismos conceptualmente en
los 3 mundos — lo que cambia es **qué tan configurable es cada paso**, no la secuencia. Eso confirma que
diseñar el wizard alrededor de estos 12 pasos (en vez de alrededor de las peculiaridades de cada
borrower) es el camino correcto — cada peculiaridad real ya vista (gate de tolerancia, fallback de
netAmount, split por saldo, etc.) es una **variación configurable de uno de estos 12 pasos**, no un paso
adicional.

---

## 4 — Próximo paso sugerido

Con este pipeline verificado, el trabajo que sigue es decidir, paso por paso, **qué tan configurable
necesita ser cada uno** para cubrir los casos reales del Grupo B/C de `mapeo-borrowers-configuracion.md`
— por ejemplo, el paso 4b (hoy 3 flags binarios) necesita evolucionar hacia el gate con tolerancia de
VPR-9633, y el paso 5a necesita los resolvers adicionales que dejamos anotados para "próxima iteración"
en VPR-9635 (Somos, Rapicredit, Finamco).
