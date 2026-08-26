# Mapeo borrower → etapas de configuración del Distribution Engine

> **Fecha:** 2026-08-19 · **Fuente de las etapas:** `docs/screen-1.png` (wizard "New Deal Setup", 9 pasos) +
> las 16 historias creadas en [VPR-9627](https://pmvaas1.atlassian.net/browse/VPR-9627).
> **Fuente de los borrowers:** los tres documentos de conciliación/distribución + `epica-distri-engine.md`.
> **Objetivo:** para cada borrower real, qué tiene que llenar en cada etapa del wizard para quedar
> configurado — y qué etapa no tiene todavía una historia que la cubra.

---

## 1 — Las 9 etapas y las 16 historias ya creadas

| # | Etapa (screen-1.png) | Historias en VPR-9627 |
|---|---|---|
| 1 | Deal Info — *Borrower, trust, nombre* | VPR-9644 Deal Info: relación borrowers con master trust |
| 2 | Pool Strategy — *How much to distribute* | VPR-9628 Payment tape · VPR-9629 Account balance · VPR-9630 Data Source Aggregation |
| 3 | Payment Filters — *Which payments to include* | VPR-9631 Accounting Payments (impacto en payment tape) · VPR-9632 distribuir Accounting Payments · VPR-9633 Conciliation Requirements · VPR-9634 Date & Time filters |
| 4 | Virtual Columns — *Computed columns* | **ninguna — ver Sección 2** |
| 5 | Distribution Rules — *Assignment pipeline* | VPR-9643 owner por componente de la cuota (con [spec en Sheets](https://docs.google.com/spreadsheets/d/1dBWaMxQWqiOgSFtv9qaFePA9HOQTuLqt1YEfQvH6h2Y)) |
| 6 | Ownership — *Resolve owner* | VPR-9635 Source (seguimos igual) · VPR-9636 Cross validation |
| 7 | Readiness Checks — *Preconditions* | VPR-9637 Preconditions · VPR-9638 Failure behavior |
| 8 | Notifications — *Alerts and reports* | VPR-9639 Events & Channels · VPR-9640 Templates (subject, recipients, cuerpo, adjuntos) |
| 9 | Review & Activate — *Confirm and activate* | VPR-9641 |

---

## 2 — Gaps detectados al cruzar contra los borrowers reales

Antes de clasificar borrower por borrower, esto es lo que salta al mirar qué necesita cada uno:

1. **Etapa 4 (Virtual Columns) no tiene ninguna historia.** Es la etapa que necesitan BIA (split ponderado
   por ~50-100 "kinds" de componente) y, en menor medida, el prerrequisito de E6 (capital/interés/mora/
   garantía). Sin esta historia, ningún borrower que derive columnas al momento de distribuir (en vez de al
   ingest) tiene dónde configurarlo.
2. **Etapa 5 (Distribution Rules) solo tiene la historia de "owner por componente de cuota".** No hay
   historia todavía para: fees/deducciones (`deductions[]`, E4), multi-moneda por regla (E9), ni la regla de
   remanente/cascada (Somos, Solvento, Finkargo). Los tres aparecen en más de un borrower real — no son
   casos aislados.
3. **Etapa 2 (Pool Strategy) ya cubre los tres casos que la épica pedía** (Payment tape, Account balance,
   Data Source Aggregation) — coincide con E3 + la pregunta de producto sobre agregación multi-fuente.
4. **Etapa 6 (Ownership) tiene "Source" y "Cross validation"** — cubre E1, pero no queda claro si
   "Source" contempla resolvers no estándar (Somos: cadena de suscripción; Rapicredit: join contra loan
   tape) o solo Payment Tape vs. Ownership API. Hay que confirmarlo al llenar VPR-9635.
5. **No hay etapa para "canal de instrucción" (E7: email vs. SFTP).** Notifications (etapa 8) cubre
   eventos/templates, pero el canal de entrega (SFTP con host-ref, PGP, etc. para Skandia/Liquitech) no
   aparece todavía en ninguna de las 16 historias.

---

## 3 — Clasificación por borrower

Convención: ✅ ya resuelto con las historias actuales · ⚠️ necesita algo que la historia debería contemplar
pero no está confirmado · ❌ necesita una etapa/capacidad que hoy no tiene historia (gap de la Sección 2).

### Grupo A — Ya 100% en el motor (nada que migrar, sirven de control)

**ADDI, ADDI_BNPN, PayJoy, Sistecredito, Welli** y los 5 de conciliación-only (**Exitus, Coograncolombiana,
Equity Link, Generandorlq, Hilco Arr.**):

| Etapa | Qué llenan |
|---|---|
| 1. Deal Info | ✅ Borrower + master trust existentes |
| 2. Pool Strategy | ✅ `PAYMENT_TAPES` (default) |
| 3. Payment Filters | ✅ filtros estándar por gateway/fecha; sin Accounting Payments |
| 4. Virtual Columns | ✅ no aplica (no derivan columnas al distribuir) |
| 5. Distribution Rules | ✅ reglas simples owner/borrower, `amountField` default |
| 6. Ownership | ✅ Source = Ownership API estándar, sin cross-check (hasta que E1 esté activo) |
| 7. Readiness Checks | ✅ los 3 checks transversales |
| 8. Notifications | ✅ email estándar |
| 9. Review & Activate | ✅ |

Estos 10 borrowers son el caso de prueba de que el wizard, tal como está, ya alcanza — úsalos para
validar VPR-9628–9641 antes de meterle los casos difíciles.

### Grupo B — Ya usan el contrato central, pero el cómputo vive en Python (Oleada 3 del backlog de unificación)

**Inklusiva**
| Etapa | Qué llenaría |
|---|---|
| 2. Pool Strategy | ✅ `PAYMENT_TAPES` |
| 3. Payment Filters | ✅ por gateway (EFECTY/PSE/WOMPI/Bancolombia Corr.), llave de match distinta por gateway → si "Payment Filters" no permite llave de match configurable por gateway, es ⚠️ a confirmar en VPR-9633/9634 |
| 5. Distribution Rules | ✅ split binario 100%-owner / 100%-Inklusiva |
| 6. Ownership | ✅ Source estándar |
| 7. Readiness Checks | ⚠️ el gate de conciliación 10% por **conteo** (no por monto) — confirmar que VPR-9637/9638 soporta ambas bases, no solo una |
| 8. Notifications | ✅ correo accionable con Excel 3-tabs — ya el patrón que Notifications busca cubrir |

**Niko**
| Etapa | Qué llenaría |
|---|---|
| 3. Payment Filters | ⚠️ dos caminos de conciliación conviven (Stripe vs. banco directo) — confirmar si Payment Filters admite dos fuentes de verdad distintas para el mismo borrower |
| 7. Readiness Checks | ⚠️ el corte 10% se mide por **monto**, no por conteo (a diferencia de Inklusiva) — mismo punto que arriba, dos bases distintas conviviendo en el mismo formulario |

**Finkargo Colombia**
| Etapa | Qué llenaría |
|---|---|
| 3. Payment Filters | ✅ PT↔FondoTransferencia, sin capa Payment |
| 5. Distribution Rules | ❌ fee/spread que absorbe Finkargo (necesita `deductions[]`, gap de Sección 2) + ❌ "dos distribuciones por corrida" (cuenta vs. tránsito) — no hay campo para esto en ninguna etapa hoy |
| 6. Ownership | ✅ ya implementa cross-check "API gana" — es la referencia de aceptación para VPR-9636 |
| 2. Pool Strategy | ⚠️ multi-moneda (USD/COP) por fila — confirmar si Pool Strategy o Distribution Rules es donde vive esto (probablemente Distribution Rules, ver E9) |

**Somos**
| Etapa | Qué llenaría |
|---|---|
| 2. Pool Strategy | ✅ `ACCOUNT_BALANCE` — Somos es el precedente real de VPR-9629 |
| 5. Distribution Rules | ❌ regla de remanente (mismo gap que Finkargo/Solvento) |
| 6. Ownership | ❌ fallback por cadena de suscripción (subir a un contrato ancestro) — no es "Source" estándar ni "cross validation", necesita confirmarse si entra en VPR-9635 o es un tercer tipo de resolver |
| 7. Readiness Checks | ❌ trigger mensual por calendario, no "cuando hay pagos" — ninguna etapa cubre frecuencia/disparador hoy; **Readiness Checks asume que la corrida ya se disparó**, no decide cuándo dispararla |

**Solvento**
| Etapa | Qué llenaría |
|---|---|
| 2. Pool Strategy | ✅ `ACCOUNT_BALANCE` (saldo − Lendable − BBVA − inconsistentes − tránsito − reserva) |
| 5. Distribution Rules | ❌ regla de remanente (mismo gap) |
| 3. Payment Filters | fuera de esta etapa: su conciliación bancaria ($1 absoluto, todo-o-nada) es del módulo de conciliación, no del wizard de distribución |

**Rapicredit**
| Etapa | Qué llenaría |
|---|---|
| 6. Ownership | ❌ resolver por join contra loan tape (`id_credito`) — no es "Ownership API" ni cross-check contra ella, es una tercera fuente de ownership que ninguna de las 2 historias contempla |
| 7. Readiness Checks | ⚠️ hoy le falta el gate de día hábil (bug, no feature) — de todos modos hay que confirmar que el wizard fuerza ese check por default, para no heredar el bug al migrar |

**JTP** (baja prioridad, desactivado)
| Etapa | Qué llenaría |
|---|---|
| 3. Payment Filters | ⚠️ "conciliación" = dedup + settlement lag por gateway (EFECTY bisemanal, PSE +1 día) — confirmar si Date & Time filters (VPR-9634) alcanza para expresar settlement lag por gateway |
| 6. Ownership | ❌ conflictos de ownership detectados se reincluyen (pendiente D0.4 del backlog de unificación: bug o feature) |

### Grupo C — Bypasean el motor hoy (no tienen ningún dato en el wizard todavía)

**BIA**
| Etapa | Qué necesitaría |
|---|---|
| 4. Virtual Columns | ❌ split ponderado por ~50-100 "kinds" de componente — es el caso que more justifica esta etapa; sin ella, BIA no tiene cómo entrar al wizard salvo con reglas simples de `amountField` |
| 7. Readiness Checks | ❌ guardrail de frescura (hard-fail si un pago no-anticipo tiene +30 días) — ningún check transversal de hoy cubre "antigüedad del pago" |
| 6. Ownership | ✅ Source = Atom API, esto sí calza |

**Finamco**
| Etapa | Qué necesitaría |
|---|---|
| 6. Ownership | ❌ owner viene de una columna de texto libre normalizada por alias — no es "Source" estándar (ni API ni tape), es un tercer tipo de resolver (mapeo por diccionario) |
| 7. Readiness Checks | ⚠️ sin gate de conciliación (fail-open por diseño) — confirmar que el wizard permite "sin tolerancia configurada" como opción válida, no solo un umbral obligatorio |

**Liquitech**
| Etapa | Qué necesitaría |
|---|---|
| 8. Notifications | ❌ canal SFTP con PGP, credencial por referencia — es el gap de "canal de instrucción" de la Sección 2, punto 5. Ya tiene ticket propio (E7) en la épica, pero el wizard no tiene hoy dónde configurar esto |

---

## 4 — Cómo seguir

El orden natural es: **validar el Grupo A primero** (ya calza 100%, sirve de control) → **llenar Distribution
Rules y Virtual Columns como historias nuevas** (son los dos gaps que más borrowers reales piden: fees,
multi-moneda, remanente, columnas derivadas) → recién ahí clasificar el Grupo B/C contra el wizard
completo, borrower por borrower, con los mismos criterios de aceptación que ya usa `epica-distri-engine.md`
(assignments byte-idénticos para los que no cambian, regresión para los que migran).

¿Querés que cree las historias que faltan (Virtual Columns como etapa 4, y las 3 sub-historias de
Distribution Rules: fees/deducciones, multi-moneda, remanente) dentro de VPR-9627, o que primero se
confirme con quien armó el wizard si esas etapas ya estaban pensadas para cubrir esto?
