# Distribución unificada — qué queremos, qué tenemos, backlog atómico

> **Fecha:** 2026-08-19 · **Alcance:** solo *distribución* (cómo se calcula y se mueve la plata). La
> *conciliación* (el matching pago↔pago) queda explícitamente afuera — es un módulo aparte, ya
> optimizado para ese propósito (Conci Engine / scripts de matching). Esta lista trata la conciliación
> únicamente como **insumo** (un FK ya escrito que la distribución puede leer como gate, E8), nunca como
> algo a construir aquí.
> **Fuentes:** síntesis de [epica-distri-engine.md](epica-distri-engine.md) (motor Kotlin, `master-trust-servicer-api`),
> [conciliacion-distribucion-estado-actual-por-borrower.md](conciliacion-distribucion-estado-actual-por-borrower.md)
> (`master-servicer-apps`, Python/Scrappy), y
> [conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md](conciliacion-distribucion-scrapy-lambdas-jtp-rapicredit-solvento.md)
> (`scrapy-lambdas`: JTP/Rapicredit/Solvento).
> **Alcance de equipo:** solo backend (los tres repos de arriba, todos backend). Lo que depende de
> pantalla/BO/wizard (coordinación ⬛ marcada en la épica) queda fuera de este backlog — no es "nuestro"
> en esta primera etapa. Cada tarea de la Sección 3 es ejecutable sin esperar a ningún frontend.

---

## 1 — Qué queremos

Una sola fuente de verdad para "cómo se distribuye la plata de un borrower", sin importar en qué repo
nació esa lógica. Hoy hay tres motores de distribución que no se conocen entre sí:

1. **El motor config-driven** (`master-trust-servicer-api`, Kotlin) — el objetivo declarado por la épica.
2. **Python/Scrappy a medida** (`master-servicer-apps`) — 8 implementaciones distintas, cada una con su
   propia noción de split, ownership y persistencia.
3. **Lambdas a medida** (`scrapy-lambdas`) — 3 implementaciones más (JTP, Rapicredit, Solvento), con su
   propio cliente de master-servicer, su propio ledger de idempotencia, su propia config de cuentas.

"Unificar" tiene dos niveles distintos, y **conviene no tratarlos como el mismo trabajo**:

- **Nivel contrato — que todo lo que mueve plata real pase por el mismo sistema de registro**
  (`DistributionPayload` / `Assignment` / la API de `master-trust-servicer-api`), sin importar dónde se
  calculó el split. Esto es barato, bajo riesgo, y cierra los tres bypasses que hoy mueven dinero real sin
  auditoría central (BIA, Finamco, Liquitech).
- **Nivel cómputo — que el split mismo se calcule en el motor Kotlin config-driven**, retirando el código
  Python/Lambda borrower por borrower. Esto es lo que persigue la épica (E1–E11), pero solo tiene sentido
  hacerlo cuando un deal lo justifica — no de oficio.

**Recomendación:** atacar el nivel contrato primero (Track A abajo). Es independiente de la épica, de bajo
riesgo, y elimina el problema más serio que ningún ticket de la épica toca hoy: plata real moviéndose sin
pasar por el motor.

---

## 2 — Qué tenemos (estado real, cruzando los tres repos)

| Borrower | Dónde se calcula el split | ¿Pasa por `DistributionPayload`/`Assignment`/API del motor? | Nota |
|---|---|---|---|
| ADDI, ADDI_BNPN, PayJoy, Sistecredito, Welli | Motor Kotlin (nativo) | ✅ | Ya unificado en ambos niveles |
| Exitus, Coograncolombiana, Equity Link, Generandorlq, Hilco Arr. | Motor Kotlin (nativo) | ✅ | El scrappy de estos 5 solo escribe FKs de conciliación — la distribución **ya** vive en el motor. Son el modelo a copiar, no a migrar |
| Inklusiva | Python (`master-servicer-apps`) | ✅ vía API | Modelo de referencia *del patrón scrappy*, pero el cómputo sigue fuera del motor |
| Niko | Python (`master-servicer-apps`) | ✅ vía API (parcial, N/A en tramo banco directo) | idem |
| Finkargo Colombia | Python (`master-servicer-apps`) | ✅ vía API | Ya implementa en Python lo que **E1** (ownership cross-check) y **E9** (multi-moneda) quieren construir en Kotlin — usar como spec |
| Somos | Python OOP (`master-servicer-apps`) | ✅ (vía `Distributor` interno) | `poolSource: ACCOUNT_BALANCE` ya funciona en Python — es el precedente de **E3** |
| Rapicredit | Python/Lambda (`scrapy-lambdas`) | ✅ vía API (`create_distribution`) | Sin gate de día hábil (bug, no feature); fórmula de balance residual con `TODO` sin resolver |
| Solvento | Python/Lambda (`scrapy-lambdas`) | ✅ vía API (`create_distribution`) | Split = saldo − partes − inconsistentes − tránsito − reserva → mismo patrón que Somos, segunda evidencia independiente de **E3**/**E4** |
| JTP (desactivado) | Python/Lambda (`scrapy-lambdas`) | ✅ vía API **+ además** `UPDATE` directo en MySQL | Doble persistencia — riesgo propio, y conflictos de ownership detectados que **se reincluyen** en vez de excluirse |
| **BIA** | Python (`master-servicer-apps`) | ❌ `UPDATE payment_tape` directo, `distribution_id` siempre `None` | **Bypass de riesgo alto** — plata real sin pasar por el motor |
| **Finamco** | Python (`master-servicer-apps`) | ❌ POST a endpoint custom hardcodeado | **Bypass de riesgo alto** — además sin gate de conciliación (fail-open) |
| **Liquitech** | Python, script manual (`master-servicer-apps`) | ❌ POST directo, arquitectura invertida | **Bypass de riesgo alto** — ya tiene ticket dedicado: **E7** de la épica |
| Vemo | ⬛ sin verificar (distribución borrada del repo 2026-05-29) | ⬛ probablemente motor Kotlin, no confirmado | Hueco de información, no de diseño |
| Haycash | N/A — nunca implementado | N/A | Housekeeping |
| Cesionbank | N/A — no es distribución (scraper mal etiquetado) | N/A | Housekeeping |

**El hallazgo que cruza los tres documentos y ningún ticket de la épica cubre hoy:** BIA, Finamco y
Liquitech mueven dinero real sin pasar por el sistema de registro central. Cualquier control que la épica
agregue en el motor (E1 ownership, E2 saldos, E8 gate de conciliación, readiness checks) **no los toca**
hasta que ese bypass se cierre — están fuera del radio de cualquier validación futura.

---

## 3 — Backlog atómico (backend, solo procesos de distribución)

Convención de tamaño: **XS** = menos de medio día (grep, lectura, confirmación con negocio, config sin
código) · **S** = 1–3 días, un PR chico, testeable solo · **M** = requiere diseño previo o toca más de un
archivo/repo de forma no trivial. Nada abajo debería ser L — si una tarea se siente L, hay que volver a
partirla.

Cada tarea es de un repo backend (`master-trust-servicer-api`, `master-servicer-apps`, `scrapy-lambdas`),
se puede tomar, entregar y probar sola, y no depende de ninguna pantalla nueva.

### Oleada 0 — Preguntas que hay que cerrar antes de estimar el resto

Ninguna de estas escribe código de producto; son grep/lectura/confirmación con negocio. Bloquean tareas
puntuales más abajo, no todo el backlog — se pueden resolver en paralelo entre sí.

| ID | Tarea | Bloquea |
|---|---|---|
| D0.1 | Grep + lectura: ¿`MasterServicerClient` (`master-servicer-apps`) y `create_distribution` (`scrapy-lambdas`) llaman al mismo endpoint de `master-trust-servicer-api`, o son dos integraciones distintas? | A1, A2, A4 |
| D0.2 | Confirmar en qué repo corre hoy la distribución de Vemo (¿motor Kotlin, o huérfana tras el borrado del 2026-05-29?) | A6 |
| D0.3 | Confirmar con negocio: ¿JTP se reactiva o se retira del roadmap? | A4, B8 |
| D0.4 | Confirmar con negocio: el re-include de conflictos de ownership en JTP, ¿es bug o decisión deliberada? | B8 |
| D0.5 | Confirmar con negocio: ¿algún deal necesita el split ponderado por ~50–100 "kinds" de BIA como capacidad genérica, o alcanza con 2–3 reglas de `amountField`? | B9 |
| D0.6 | Confirmar con negocio: ¿`add_rapicredit_balance` (con el `TODO: PERCENT is missing`) está conectada al flujo real de Rapicredit, o es código muerto? | B7 |
| D0.7 | Confirmar con negocio: ¿el fee mensual de Somos puede esperar a la próxima corrida de distribución, o necesita disparador propio (pedido #4)? | B6 |

### Oleada 1 — Cerrar los bypasses de plata real (BIA, Finamco, JTP)

Nadie espera a esto: no toca el motor Kotlin, no requiere ninguna capacidad nueva, solo hace que cada
borrower registre en el sistema central lo que ya calcula. Es la oleada de mayor ROI de todo el documento.

**BIA** (`master-servicer-apps`) — depende de D0.1:
| ID | Tarea | Tamaño |
|---|---|---|
| A1.1 | Leer el payload exacto que Inklusiva/Finkargo ya envían al crear una distribución (forma de `DistributionPayload`/`Assignment`) — es el contrato a replicar | XS |
| A1.2 | Construir ese payload a partir del split ya calculado de BIA (ponderado por componente), sin tocar la lógica de cálculo | S |
| A1.3 | Reemplazar la llamada a `marcar_distribuidos` (`UPDATE payment_tape` directo) por el registro vía el contrato de A1.1 | S |
| A1.4 | Test de regresión: correr BIA en stg sobre un set histórico y comparar filas marcadas por el camino viejo vs. el nuevo | S |
| A1.5 | Retirar el código de `UPDATE` directo una vez validado A1.4 | XS |

**Finamco** (`master-servicer-apps`) — depende de D0.1:
| ID | Tarea | Tamaño |
|---|---|---|
| A2.1 | Construir el payload `DistributionPayload`/`Assignment` a partir de los dos "planos" JSON que Finamco ya calcula | S |
| A2.2 | Reemplazar el POST al endpoint custom hardcodeado por el registro vía el contrato central | S |
| A2.3 | Test de regresión sobre un set histórico de Finamco | S |
| A2.4 | Retirar el endpoint custom y el POST hardcodeado una vez validado A2.3 | XS |

**JTP** (`scrapy-lambdas`) — solo si D0.3 confirma que sigue vivo:
| ID | Tarea | Tamaño |
|---|---|---|
| A4.1 | Eliminar la escritura directa a MySQL `payment_tape`; dejar el registro vía API como única fuente de verdad | S |

**Liquitech** — ya tiene ticket propio en la épica: **E7** (borra `PostDistribution.kt`, 534 líneas). No
se duplica acá, solo se enlaza.

**Housekeeping** (`master-servicer-apps`) — depende de D0.2:
| ID | Tarea | Tamaño |
|---|---|---|
| A5.1 | Eliminar o re-etiquetar la carpeta `Cesionbank` (scraper clonado de BIA, mal declarado `BIA_DISTRIBUTION` en BPM, con bug de runtime `self` como función de módulo) | XS |
| A5.2 | Eliminar o documentar como muerto el scaffolding de `Haycash` (nunca implementado) | XS |
| A5.3 | Si D0.2 confirma que Vemo quedó huérfana: decidir y ejecutar dónde vive su distribución (si no, cerrar sin cambios) | XS–S |

### Oleada 2 — Capacidades base del motor Kotlin

Esto **ya está atomizado en la épica** (E1, E2, E3, E9a, readiness checks) — no se repite acá. Es la
oleada 1 de `epica-distri-engine.md`. Se lista solo como punto de enganche porque toda la Oleada 3 de este
documento depende de que estas capacidades existan.

### Oleada 3 — Migrar cómputo al motor, por borrower (una vez la Oleada 2 está lista)

**Finkargo Colombia** — depende de E1, E9b (épica):
| ID | Tarea | Tamaño |
|---|---|---|
| B1.1 | Extraer de `FinkargoColombiaDistributor` (`task.py:531, 819-824`) la lógica de cross-check owner-vs-API como caso de test de aceptación para **E1** | XS |
| B1.2 | Configurar Finkargo en el motor con estrategia `API_WINS` una vez E1 esté en prod | S |
| B2.1 | Extraer de Finkargo la lógica de agrupación por moneda como caso de test para **E9b** | XS |
| B2.2 | Configurar Finkargo en el motor con soporte multi-moneda una vez E9b esté en prod | S |
| B2.3 | Migrar el fee/spread que absorbe Finkargo a `deductions[]` (**E4**) | S |
| B2.4 | Investigar si el motor soporta "dos distribuciones por corrida" (cuenta vs. tránsito) o si es una capacidad nueva a especificar | S (investigación) |

**Somos** — depende de E3, D0.7:
| ID | Tarea | Tamaño |
|---|---|---|
| B3.1 | Configurar `poolSource: ACCOUNT_BALANCE` para Somos una vez E3 esté en prod | S |
| B3.2 | Diseñar el resolver de ownership por cadena de suscripción (evaluar si es una estrategia más de E1 o un resolver nuevo) | M |
| B3.3 | Resolver el trigger mensual por calendario, según lo que responda D0.7 (frecuencia nueva en `DistributionFrequencyChecker`, o disparador del pedido #4) | S (tras D0.7) |

**Solvento** (`scrapy-lambdas` → motor) — depende de E3, E4:
| ID | Tarea | Tamaño |
|---|---|---|
| B5.1 | Configurar `poolSource: ACCOUNT_BALANCE` para Solvento una vez E3 esté en prod | S |
| B5.2 | Migrar la regla de remanente (`Lendable`/`BBVA` capados al depósito, resto a Solvento) a **E4** | S |
| — | La conciliación bancaria ($1 absoluto, todo-o-nada contra Actinver) **no migra acá** — es del módulo de conciliación | — |

**Rapicredit** (`scrapy-lambdas`) — sin depender de la épica, se puede hacer ya:
| ID | Tarea | Tamaño |
|---|---|---|
| B6.1 | Agregar el gate de día hábil que hoy falta (bug, no feature) | S |
| B7.1 | Si D0.6 confirma que la función está viva: arreglar el `TODO: PERCENT is missing` de la fórmula de balance residual | S |

**JTP** — depende de D0.3, D0.4:
| ID | Tarea | Tamaño |
|---|---|---|
| B8.1 | Si D0.4 confirma que reincluir conflictos de ownership es un bug: excluir esas filas en vez de reincluirlas | S |

**BIA** — depende de D0.5:
| ID | Tarea | Tamaño |
|---|---|---|
| B9.1 | Si D0.5 confirma la necesidad: especificar el motor no-code de columnas virtuales como ticket propio (fuera de esta épica, tamaño M–L a definir aparte) | — |

**Inklusiva / Niko** — sin depender de nada técnico, solo de priorización:
| ID | Tarea | Tamaño |
|---|---|---|
| B10.1 | Decisión de negocio: ¿vale migrar su split a Kotlin (E1+E8) dado que ya funcionan y no bloquean ningún deal? — **no crear tareas de ejecución hasta que esto se responda que sí** | — |

---

## 4 — Cómo se relaciona con las preguntas de producto de la épica

Las 6 preguntas de [epica-distri-engine.md §"Las preguntas de producto que ordenan la épica"](epica-distri-engine.md)
siguen siendo las que ordenan la **Oleada 2** (qué capacidad construir primero en Kotlin) y, por
consiguiente, cuándo puede arrancar la **Oleada 3**. Las preguntas de la **Oleada 0** de este documento son
nuevas — nacen de mirar los tres repos backend juntos, no solo `master-trust-servicer-api`.

**Orden de ejecución sugerido:** Oleada 0 (preguntas, en paralelo, no bloquea nada más allá de sus tareas
puntuales) → **Oleada 1 ya**, en paralelo a la épica, porque no depende de ninguna capacidad nueva y cierra
el mayor riesgo (BIA/Finamco/JTP moviendo plata real sin registro central) → Oleada 2 (la épica, en su
propio orden de Olas 1/2/3) → Oleada 3, borrower por borrower, cada uno cuando su capacidad de la épica
esté en prod.
