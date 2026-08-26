# Épica — Distri Engine: distribuciones que se adaptan al negocio real

> **OKR:** Selfservice ops + Profitability · **Área:** Payments — Distribución · **Superidea:** sí
> **Fecha:** 2026-08-02 · Fuentes: brief de producto (hitos 1-5) + **brief Fase 2 del BackOffice**
> (lo resaltado en verde, ver Trazabilidad) + handoff de deals scrappy + verificación contra código
> ([analisis-9-pedidos-repos.md](analisis-9-pedidos-repos.md) · [implicancias-implementacion-9-pedidos.md](implicancias-implementacion-9-pedidos.md))

---

# PARTE 1 — Resumen ejecutivo

**El problema:** cada deal nuevo trae reglas de distribución que el motor no soporta. La respuesta hoy es
desarrollo a medida — demoras, dependencia de tech, o el deal no entra. **Hilco, Exitus Engen y Nissan
están bloqueados por esto.**

**La apuesta:** 11 frentes que convierten el motor de *código por deal* a *configuración por deal*.

| # | Frente | Qué desbloquea |
|---|---|---|
| 1 | **Validar el dueño antes de pagar** | Hoy, si el archivo del cliente dice un dueño y nuestro registro dice otro, se paga igual. Se acabó la plata al destinatario equivocado |
| 2 | **Saldos de cuenta confiables** | La distribución ve la caja real del fideicomiso, no un dato que a veces está |
| 3 | **El monto a distribuir se configura** | Deals donde la plata sale del saldo de la cuenta, no de la suma de pagos |
| 4 | **Honorarios se cobran solos** | VAAS y la fiduciaria cobran desde el fideicomiso maestro sin intervención manual |
| 5 | **Pagos contables no mueven caja** | Castigos y condonaciones se marcan y no se distribuyen por error |
| 6 | **Cada parte de la cuota a su dueño** | El capital al fondeador; impuestos o seguros a quien deba pagarlos — el caso Hilco/Nissan |
| 7 | **La instrucción llega como el banco la exige** | Por correo o directo al sistema del banco, según lo pida cada banco — el caso Skandia |
| 8 | **La conciliación decide si se distribuye** | Si el cruce de pagos falla más de lo tolerado, la corrida se frena y avisa; si pasa, distribuye e informa las diferencias al cliente. Hoy eso vive en scripts, con el umbral escrito en el código |
| 9 | **Una instrucción, dos monedas** | Deals que liquidan en pesos y dólares en la misma corrida. Hoy no se soporta — y si se mezclan, el sistema las suma como si fueran una sola |
| 10 | **La corrección del cliente reemplaza al error** | Cuando el cliente re-sube el archivo corregido, hoy o se descarta en silencio o queda duplicado y genera un falso "pago sin conciliar" |
| 11 | **Los correos se editan sin tech** | Asunto, cuerpo y adjuntos configurables, con plantillas — incluidos los casos a medida (Crediorbe, Somos) |

**Resultado:** el pipeline bloqueado entra, el equipo de implementaciones configura distribuciones sin
depender de tech, y el sistema valida antes de mover plata (dueño correcto, insumos completos, sin corridas
dobles) en vez de corregir después.

**Punto de partida:** la pantalla de configuración ya está construida y gran parte del motor ya existe — la
mayoría de los frentes cierran lo que falta sobre piezas que ya funcionan, no construyen de cero.

---

# PARTE 2 — Desglose técnico (para el equipo)

> **Convenciones:** tamaños S/M/L como en [implicancias-implementacion-9-pedidos.md](implicancias-implementacion-9-pedidos.md).
> ⬛ = repo no clonado. Todos los cambios de master-trust requieren deploy (rama → Jenkins dev/stg → prod).
> Los `archivo:línea` de abajo están verificados contra el código (2026-07-31/08-02).

## Prerrequisito declarado: la "primera iteración de la DB de Payments"

El brief lo nombra como prerrequisito y **es trabajo que ya está especificado fuera de esta épica**
(pedidos #2 y #1 de la Parte 1 de implicancias): bindear `fee_amount` en el extractor, limpiar los
homónimos del diccionario (`current_interest` mapeado por 5 clientes CURRENT cae hoy a un JSON), y
des-deprecar los campos de componente. **Solo E6 depende de esto.** Los otros 6 tickets no.

## Mapa de dependencias y secuencia

```
Ya en curso (fuera de esta épica):
└─ DB de Payments (#2 fee_amount + #1 homónimos)  ──────────────┐
                                                                 │
Ola 1 — protege la plata, no depende de nada:                    │
├─ E1  Validación de ownership (D4)                              │
├─ E2  Saldos confiables (#9)  ──┐                               │
├─ E9a Guardrail multi-moneda (falla en vez de sumar mal)        │
├─ E7a Higiene Liquitech (auth + lambda + resiliencia)           │
└─ E11a Adjuntos configurables (lo único no-⬛ de E11)            │
                                 │                               │
Ola 2 — habilita deals:          ▼                               ▼
├─ E3  Pool configurable (D1) — necesita E2                 E6  Componente de cuota (D2)
├─ E4  Fees desde el master (D5)
├─ E5  Pagos contables, alcance (a) (D3)
├─ E8  Conciliación como gate — necesita el force del pedido #5 (override);
│      por deal, que el matching ya escriba FKs en plataforma
└─ Readiness checks (transversal, arranca con el guard anti-doble; E8 es el cuarto check)

Ola 3 — cierra la épica:
├─ E7b Instrucción multi-canal config-driven (valida Skandia, retira el adapter de Liquitech)
├─ E9b Soporte real multi-moneda (un assignment por moneda + subtotales)
└─ E10 Re-carga del PT corregido (extractor — coordinar con el trabajo de la DB de Payments)

Fuera de alcance (explícito):
├─ D3(b) reducir OPB — el saldo del crédito vive en el core del cliente, no acá
├─ D1 agregación multi-fuente — hasta que un deal la pida
├─ Motor no-code de columnas virtuales — ticket propio (M–L) solo si la pregunta 1 lo exige;
│  mientras tanto se deriva al ingest con el pipeline que ya existe
├─ Entidad/tabla de Fees — el assignment ya es el tracking
├─ Lógica custom de conciliación — es del Conci Engine (backoffice), no de master-trust
├─ Conversión FX / TRM — E9 soporta dos monedas, no las convierte
├─ Cuerpo del correo — ⬛ bloqueado por notifications-api (E11, pregunta 5)
└─ Instrucción de Somos — su distribución la hace un scraper externo

Coordinación ⬛ BO (no aparece como flecha pero es real):
E2 (¿pantalla para el PUT de saldos?) · E4 (contrato del wizard si hay múltiples
beneficiarios) · E7b (tab 5 "Transfer Instructions" del wizard)
```

---

## E1 — Validación de ownership antes de distribuir (hito 4 del brief · D4)

> *"Si el owner del payment tape no coincide con la API, se bloquea. Hoy se distribuye igual."*

**Repo:** `master-trust-servicer-api` · **Tamaño: S–M** · **El de mejor relación valor/esfuerzo: previene
plata mal enviada, y la infraestructura que necesita ya está inyectada.**

### Estado hoy (verificado)

- La única validación es "owner no es UNKNOWN/UNDEFINED": `DistributorCalculator.kt:39-40` particiona y
  el resto **se distribuye igual**; los ownerless solo se notifican (`DistributionRunner.kt:213`, evento
  `DISTRIBUTION_UNKNOWN_OWNER_PAYMENTS`).
- No hay cross-check tape-vs-API porque corre **un solo resolver** por config
  (`OwnerNameResolverProvider.kt:20-31`, un `when` sobre `resolverType`).
- **La infra ya está:** los dos resolvers (`ByPaymentTape`, `ByOwnershipApi`) son constructor-args de la
  misma clase. El cross-check es un método nuevo en una clase que ya tiene ambas dependencias.
- Bug de paso: `p.ownerName!!` (`DistributorCalculator.kt:40`) — un tape con owner null tira NPE y
  **tumba la corrida entera** en vez de caer en la partición de ownerless.

### Qué implica

1. Método de cross-check en `OwnerNameResolverProvider`: resolver por ambas fuentes, comparar.
2. Enum de estrategia de mismatch en `config_json.ownership`: `API_WINS` / `TAPE_WINS` / `BLOCK_PAYMENT` /
   `BLOCK_DISTRIBUTION`. "Bloquear ese pago" **reusa la partición de ownerless que ya existe**, con su
   mismo evento de notificación.
3. Opt-in por deal + fallback explícito ante Ownership API caída (timeout/reintentos definidos) — que la
   API caída no frene la distribución de todos los clientes.
4. Reemplazar el `!!` de `:40` por fallback a la partición.

### Criterios de aceptación

- [ ] Tape con owner ≠ API y estrategia `BLOCK_PAYMENT` → queda fuera del pool y notificado con el evento existente.
- [ ] Estrategia `API_WINS` → se distribuye al owner de la API; `TAPE_WINS` → al del tape.
- [ ] `BLOCK_DISTRIBUTION` → la corrida no crea distribución y notifica el motivo.
- [ ] Ownership API caída → se aplica el fallback configurado y sale alerta; la corrida no muere por timeout.
- [ ] Tape con owner null → cae a ownerless (no NPE). Test de regresión sobre la partición actual.
- [ ] Deal sin la validación habilitada → comportamiento idéntico al actual.

### Riesgos / decisiones

- Mete una dependencia externa en el camino crítico de deals que hoy no llaman a la API → por eso opt-in.
- Decidir el default de fallback (recomendado: fuente configurada + alerta).

---

## E2 — Saldos de cuenta confiables (#9)

**Repo:** `master-trust-servicer-api` · **Tamaño: S** · **Es el insumo de E3.**

### Estado hoy (verificado)

- **El fix es un swap de método.** `GetAccountBalanceByDate` (`AssignmentsResolver.kt:575-585`, con
  `// TODO: THIS DOESN'T WORK. FIX THIS.` en `:581`) busca el saldo con **igualdad estricta de día**
  (`DATE(creation_date) = :date`, `AccountBalanceRepository.kt:179-185`). Saldo cargado ayer → query
  vacía → el balance-check loguea "was null" y **no protege nada**.
- El método correcto **ya existe en el mismo repositorio**: `getAccountBalanceByAccountIdAndMinDateTime`
  (`AccountBalanceRepository.kt:54-58`) y la query "más reciente por cuenta hasta la fecha" (`:105-117`).
- La escritura ya está cubierta: `PUT /master-trusts/balances` (`AccountBalanceController.kt:16`, con
  auditoría vía `X-User-Id` → `modified_by`, historial completo porque cada update inserta fila nueva), y
  el extractor escribe `account_balance` directo desde los scrapers bancarios
  (`MasterServicerRepository.java:55-100` en `payment-data-extractor`).

### Qué implica

1. Cambiar `GetAccountBalanceByDate` a "último saldo con `creation_date` ≤ fin del día de distribución"
   usando el método existente (~5 líneas + tests).
2. Política de frescura: usar el último saldo + **alertar si tiene más de 48h** (precedente: la validación
   de frescura del scraper de Sistecredito).
3. ⬛ Confirmar si el BO tiene pantalla para el `PUT`; si no, es un form de ellos contra un endpoint que ya está.
4. **No construir la vía S3** — un upload de CSV en el BO contra el `PUT` es más barato y con validación visual.

### Criterios de aceptación

- [ ] Saldo cargado ayer por `PUT` + distribución hoy con `balance-check enabled` → el check usa ese saldo (verificable en el log de `usableBalance`/`reservedBalance`).
- [ ] Saldo insuficiente → dispara `INSUFFICIENT_BALANCE` (hoy no dispara porque el saldo llega null).
- [ ] Saldo con >48h de antigüedad → se usa y sale alerta.
- [ ] Cuenta sin ningún saldo histórico → mismo comportamiento actual (null + log), no explota.
- [ ] (si el BO construye la pantalla) Una carga por pantalla/CSV entra vía el `PUT` existente y queda auditada (`modified_by` con el usuario real).

### Riesgos

- **Cambio de comportamiento real:** deals que hoy "pasan de largo" el check (saldo null) van a empezar a
  chequear contra saldos viejos. Revisar en stg qué deals tienen `balance-check enabled` antes de prod.

---

## E3 — Pool separado de asignación (hito 1 del brief · D1)

> *"De dónde viene la plata y a quién va son dos preguntas distintas. Hoy están mezcladas."*

**Repo:** `master-trust-servicer-api` · **Tamaño: S** (flag de 2 valores) · **Depende de E2.**

### Estado hoy (verificado)

- El monto sale siempre de la misma línea: `val totalAmount = paymentTapes.map { it.netAmount }.fold(...)`
  (`AssignmentsResolver.kt:183`, dentro de `processAssignmentRule` `:178-203`) — la función recibe los
  tapes que matchearon el criterio **y** calcula el monto sumándolos (menos la reserva del E4, si está
  configurada, `:184-192`). Pool y reparto comparten función.
- El saldo de cuenta **nunca es fuente**, solo validación (`balanceCheck`, `:133-168`, **3 estrategias
  vivas** — una cuarta está comentada con `// TODO: DELETE THIS.` y `@Deprecated(level = ERROR)`) — y el
  saldo ya está cargado por cuenta en el `AssignmentsAccountsBalanceManager` (`:320-363`).
- Trazabilidad ya resuelta: el assignment lleva `paymentTapes` (`:201`) independiente del monto.

### Qué implica

1. Campo `poolSource` en la config del assignment: `PAYMENT_TAPES` (default = comportamiento actual, cero
   cambio para deals existentes) | `ACCOUNT_BALANCE`. En `processAssignmentRule`, elegir el total según
   ese campo — el saldo ya está disponible ahí.
2. Definir qué pasa cuando el pool (saldo) no alcanza para todos los tiers. Matiz verificado: **solo
   LENDER y BORROWER comparten el saldo mutable** (`:56-64`); el tier "resto" (`buildRestOfTheAssignments`,
   `:77-81`) **no recibe el balance manager ni hace balance check**. Con pool=saldo, ese tier sin chequeo
   pasa a ser un agujero de sobre-giro — hay que dárselo o excluirlo explícitamente. Documentar y testear.
3. **Agregación multi-fuente: no.** Se agrega cuando exista el deal que la pida.

### Criterios de aceptación

- [ ] Deal con `poolSource: ACCOUNT_BALANCE` → el monto distribuido es el saldo de la cuenta configurada, no la suma de tapes; los tapes siguen linkeados al assignment.
- [ ] Todos los deals existentes (sin el campo) → assignments **byte-idénticos** a antes (test de regresión).
- [ ] Pool insuficiente para todos los tiers → comportamiento definido y notificado, sin sobre-giro.
- [ ] Test sobre `processAssignmentRule` (es LA función que decide plata).

### Riesgos

- Un tape que matchea 2 reglas se cuenta en ambas (`:206-230`, sin first-match-wins). Con pool=saldo eso
  es doble gasto del mismo saldo → el check de no-exceder-pool es parte del ticket, no opcional.

---

## E4 — Fees y honorarios desde el master trust (hito 5 del brief · D5)

> *"VAAS, fiduciaria y otros pueden cobrar desde el master. Hoy requiere intervención manual."*

**Repo:** `master-trust-servicer-api` · **Tamaño: S–M** · **La deducción ya funciona; falta pagarle al
beneficiario.**

### Estado hoy (verificado)

- **La deducción del pool ya funciona, no-code, por deal:** `config.reserveAmount` →
  `AdjustmentsConfig(FUNDS_RESERVATION)` (`AssignmentsResolver.kt:287-292`) → se resta del total antes de
  asignar (`:184-193`). Wimo la usa en producción (21.575 MXN).
- El modelo de costos ya existe **con porcentaje**: `CostDetail(value, description, type)` +
  `enum CostType { PERCENT, AMOUNT }` (`Payment.kt:139-155`); Wimo ya produce cost details
  (`WimoDistributor.kt:47-70`) y se persisten.
- Gaps: el monto retenido **no se le paga a nadie** (queda en la cuenta); `AdjustmentType` tiene un solo
  valor (`AssignmentsModel.kt:48-50`), solo monto fijo; y la lectura por SQL nativo **tira los cost
  details** (`NativeSQLPaymentTapeReadRepository.kt:161` y `NativeSQLPaymentReadRepository.kt:289`:
  `costDetails = emptyList(), // TODO`).

### Qué implica

1. **Que el adjustment emita su assignment contraparte:** `toAccountCode` (cuenta del beneficiario) en
   `AdjustmentsConfig` → se genera un `DistributionAssignment` con concepto que nombra el fee.
   **El assignment ES el tracking**: se persiste, sale en el Excel y en la instrucción. Sin entidad Fee,
   sin tabla de fees.
2. Porcentaje: reusar `CostType.PERCENT`.
3. Múltiples beneficiarios: campo **nuevo** opcional `deductions: []` — no tocar `reserveAmount` (Wimo
   sigue idéntico y el contrato del wizard crece sin romperse).
4. Arreglar el mapeo de `cost_details` en el camino nativo **solo si** los fees deben salir en reportes
   que usan SQL nativo.
5. **Cascada de pagos (del brief Fase 2, verde):** qué hacer con el **remanente** cuando el pool es un saldo
   bancario (E3). Precedente reusable: Sistecredito **ya manda el remanente al borrower**, hardcodeado en su
   adapter — generalizar eso a una regla de remanente es el camino, no inventar uno. Depende de E3.
6. **Pagos recurrentes desde el master (del brief, verde):** hoy toda deducción ocurre *dentro* de una
   corrida de distribución. Un honorario mensual que no depende de que haya pagos necesita **disparador
   propio** → se apoya en el pedido #4 (trigger por evento) en vez de un scheduler nuevo. Si el fee puede
   esperar a la próxima corrida, esto no hace falta: **confirmar con negocio antes de construirlo.**

### Criterios de aceptación

- [ ] Deal con fee de VAAS configurado → assignment al beneficiario por el monto deducido, con concepto identificable.
- [ ] `sum(assignments) == pool` exacto (deducción y pago atómicos — si el pago falla, la deducción no queda huérfana).
- [ ] Fee porcentual: caso de test numérico (p. ej. pool 1.000.000, fee 1.5% → assignment de 15.000 al beneficiario, 985.000 distribuidos).
- [ ] El fee aparece identificable en el Excel de distribución y en la instrucción de transferencia.
- [ ] Deal con `reserveAmount` actual (Wimo) → comportamiento intacto (la reserva sin beneficiario sigue siendo válida).

### Riesgos / dependencias / preguntas abiertas

- Es plata que sale del pool: si la deducción corre y el assignment contraparte falla, la diferencia queda
  huérfana y aparece como descuadre en conciliación. Atomicidad obligatoria + test.
- ⬛ **Dependencia BO:** múltiples beneficiarios cambian la forma de `config_json` → coordinar el contrato
  con el wizard antes de mergear.
- **Pregunta abierta (producto, decidir antes de codear):** la base del % — ¿pool total o assignment del
  owner? Dueño de la decisión: producto, con el primer deal que use fee porcentual como caso de referencia.

---

## E5 — Pagos contables sin respaldo de caja (hito 3 del brief · D3, alcance a)

> *"Ajustes contables que reducen OPB pero no representan efectivo. Hoy no se soportan."*

**Repo:** `master-trust-servicer-api` · **Tamaño: S (posiblemente 0 código)** · **Partido en dos a
propósito; acá va solo (a).**

### Estado hoy (verificado)

- Confirmado inexistente: `grep -riE "accounting|contable|castigo|opb|nonCash"` sobre todo `src/main` →
  **0 hits**. El motor es 100% caja: todo tape del pool genera assignment y todo assignment mueve plata.
- Pero **"no los distribuyas" ya es configurable**: los `distribution-filters` de `distributablePayments`
  ya filtran por campos del tape — Wimo filtra por `extraData.depositDate` (`DISTRIBUTE_BY_DATE`).

### Qué implica

1. Definir la **marca** de pago contable en el tape (columna nueva o `aux_var` → `extra_data`, según qué
   mande cada cliente — se decide con el mapeo del deal).
2. Excluirlo del pool con el mecanismo de filtros existente. Si la marca viene en un aux var, esto es
   **config, cero código**.
3. Que aparezca en el reporte de no-distribuidos con su razón (el Excel de distributed-and-undistributed
   ya existe).
3b. **Matiz del brief Fase 2:** la decisión de distribuir o no **depende de si el owner es un lender o el
   borrower**. Eso NO requiere mecanismo nuevo: el `criteria` de las reglas de assignment ya discrimina por
   owner y los tiers LENDER / BORROWER ya existen como concepto de primera clase en el resolver
   (`AssignmentsResolver.kt:56-68`). Es una condición más en el filtro, no un modelo nuevo.
4. **(b) "reducir OPB" queda explícitamente fuera:** el saldo del crédito vive en el core del cliente /
   borrowers-core, no en master-trust. Hasta definir dueño, master-trust solo debe *no tocar* esos pagos.

### Criterios de aceptación

- [ ] Tape marcado como contable → no aparece en el pool ni en ningún assignment.
- [ ] Aparece en el Excel de no-distribuidos con la razón "pago contable".
- [ ] Un deal sin la marca configurada → comportamiento idéntico al actual.
- [ ] El filtro es defensivo por default: ante duda de marca, **no** distribuir (un contable distribuido es un descuadre bancario).

### Pregunta abierta (define el roadmap)

- ¿Alguno de los deals del pipeline bloqueado (Hilco, Exitus Engen, Nissan) necesita (b) — reducir OPB —
  o a todos les alcanza (a)? Es la diferencia entre S y L, y entre un repo y varios.

---

## E6 — Distribución por componente de cuota (hito 2 del brief · D2)

> *"Capital, intereses, impuestos y seguros pueden ir a owners distintos. Hoy todo va al mismo."*

**Repos:** `master-trust-servicer-api` (el split, chico) + `payment-data-extractor` + ⬛ diccionarios
(el dato, grande) · **Tamaño: S el split · L el prerrequisito de datos** · **El único ticket bloqueado
por la DB de Payments. Es el caso Hilco/Nissan.**

### Estado hoy (verificado)

- Los 4 componentes **existen en las 3 capas**: dominio (`Payment.kt:119-126` — `currentPrincipal`,
  `currentInterest`, `moratoryInterest`, `currentGuarantee`), entidad (`PaymentTapeEntity.kt:88-97`),
  persistencia (`SavePaymentTapeQuery.kt:158-161`). El camino de lectura ORM que usa la distribución los
  trae bien (MapStruct mapea por nombre).
- ⚠️ Los 4 están **`@Deprecated("Use feeAmount instead.")`** — mensaje erróneo (el capital no es un fee).
  Hay que revertir la deprecación: es la dirección opuesta a este hito.
- **Quién los lee hoy: solo Sistecredito**, y `current_guarantee` es load-bearing ahí:
  `partition { it.currentGuarantee == null }` (`SistecreditoDistributor.kt:71`) parte el flujo entero, y
  `:121-126` lo resta del netAmount. **Regresión obligatoria sobre Sistecredito.**
- **El dato de entrada está roto** (por eso el prerrequisito): `fee_amount` es NULL siempre (el campo del
  diccionario cae al JSON — no está bindeado en el DTO del extractor), y `current_interest` cae al JSON
  para 5 clientes CURRENT que creen estar mandando interés.
- **La maquinaria de fórmulas ya existe y corre en prod:** el `post-mapper-pipeline` del extractor — XIMPLE
  deriva `paymentInterest`/`paymentFees` de `payment_detail` con filters + aggregations
  (`business-config.yml:804-816`). El brief pide "columnas virtuales y fórmulas": **copiar esa semántica,
  no inventarla.**

### Qué implica

1. **Prerrequisito (fuera de este ticket, ya especificado):** DB de Payments — bindear `fee_amount`,
   limpiar homónimos, revertir los `@Deprecated`, backfill si negocio lo pide.
2. **El split (~5 líneas):** `amountField` opcional en `AssignmentRule` (default `netAmount`) y sumar ese
   campo en `:183`. Un deal "capital al lender, impuestos al borrower" = **dos reglas** con el mismo
   criterio y distinto `amountField` — la forma que el motor ya tiene. Reusa criteria, reglas,
   balance-check y adjustments tal cual.
3. **El check nuevo obligatorio:** suma de componentes asignados ≤ `netAmount` del tape. Hoy un tape que
   matchea 2 reglas se cuenta 2 veces (bug latente); con componentes eso pasa a ser la feature, pero sin
   este check se distribuye plata que no existe.
4. **Decisión de diseño — dónde derivar componentes que no vienen en el tape:** al ingest (pipeline del
   extractor: ya existe, YAML, requiere deploy) vs. al distribuir (nuevo, `config_json`, no-code — lo que
   pide el brief). Recomendación: deals que se puedan resolver al ingest, resolverlos ahí mientras el
   motor no-code se construye — desbloquea antes. **El motor no-code de columnas virtuales NO tiene ticket
   en esta épica**: se abre como ticket propio (M–L) solo si la respuesta a la pregunta 1 lo exige.
5. **Alcance honesto de "impuestos y seguros":** las 4 columnas existentes son capital / interés / mora /
   garantía. Impuestos y seguros **no son columnas hoy** — llegan por derivación (punto 4) hacia una de
   las existentes, un `aux_var`, o una columna nueva (migración chica). El primer deal que los necesite
   define cuál de las tres.

### Criterios de aceptación

- [ ] Deal con dos reglas sobre el mismo criterio (una `currentPrincipal`, otra `currentInterest`) → dos assignments a owners distintos.
- [ ] Suma de componentes asignados de un tape ≤ su `netAmount`; si excede, la corrida falla antes de crear assignments, con detalle.
- [ ] Tape sin el componente poblado (null) → comportamiento definido (¿0? ¿excluir? ¿fallar?) y notificado.
- [ ] Sistecredito: regresión completa intacta (partición por `currentGuarantee` y resta del netAmount).
- [ ] Deals existentes sin `amountField` → assignments idénticos a antes.

### Riesgos

- Construir el split antes del prerrequisito = feature que suma NULLs. **No arrancar E6 hasta que
  `fee_amount`/`current_interest` lleguen a sus columnas para los deals que lo van a usar.**

---

## E7 — Instrucción de distribución multi-canal (caso Liquitech → Skandia)

**Repo:** `master-trust-servicer-api` · **Tamaño: E7a = S–M · E7b = L** · **En dos fases con valor propio.**

### Estado hoy (verificado)

- La instrucción estándar **solo reconoce email**: `method.equals("email")` hardcodeado
  (`MasterServicerNotification.kt:153`); cualquier otro `method` se ignora en silencio, sin rama else. Cualquier otro canal corre solo por el camino hardcodeado de
  Liquitech: `core/scrappy/liquitech/PostDistribution.kt` — **534 líneas**, cuentas/NIT/secuencias como
  constantes (`:503-521`), credencial SFTP pegada (`:523`), y
  `// TODO: Make this more resilient. If this fails, nothing will execute this automatically again` (`:131`).
- El endpoint que lo dispara está **sin auth**: `@VaasSecurity` comentado con
  `// TODO: REACTIVATE THIS LATER!!!` (`ScrappyLiquitechController.kt:20-21`).
- **La infra SFTP ya existe y está probada:** `SFTPSender` vía Lambda con credencial por referencia — pero
  el nombre de la lambda está hardcodeado a prod (`infra/sftp/Sender.kt:63`): **dev y stg pegan contra
  prod**, no se puede probar nada de esto sin arreglarlo primero.
- Destinatarios de email hardcodeados: 6 direcciones en `InformNewByEmail.kt:60`.
- Adjuntos ya funcionan (URLs prefirmadas de S3 a documentos del documents-api).

### E7a — Higiene (entregable ya, alivia ops)

1. Reactivar `@VaasSecurity` (coordinar con el caller externo — el scraper de Liquitech).
2. Lambda SFTP por property de ambiente (prerrequisito de testear todo lo demás).
3. Emails a config (patrón `{BORROWER}_..._EMAIL_RECIPIENTS` en Secrets Manager que ya usan 7 clientes).
   Las cuentas/NIT se quedan: nunca cambiaron.
4. Resiliencia del post: reintento con backoff + evento de error SFTP (existe el de email:
   `DISTRIBUTION_TRANSFER_INSTRUCTION_ERROR`; falta el equivalente) + idempotencia de envío
   (base: `assignmentWasAlreadyMade`, `:288`).

### E7b — El hito de la épica

5. Reemplazar `method: String` por lista de canales en `TransferInstructionConfig`: `EMAIL` (recipients,
   subject) y `SFTP` (`host-ref` a credencial, `remote-path`, `filename` con `${date}`), reusando
   `SFTPSender` tal cual. Forma propuesta en
   [distri-engine-estado-tecnico-real.md](distri-engine-estado-tecnico-real.md) §4.
6. Migrar Liquitech al canal por config y **borrar `PostDistribution.kt`** (las 534 líneas). Retirar este
   adapter es la métrica de éxito de la épica.
7. **Skandia estrena el canal** (siempre necesita SFTP): es el deal de validación.

### Criterios de aceptación

- [ ] (E7a) El endpoint devuelve 401 sin token y el flujo de Liquitech sigue funcionando.
- [ ] (E7a) dev/stg pegan a su propia lambda SFTP; cambiar un destinatario de email es config, no PR.
- [ ] (E7a) Un fallo simulado del post reintenta, no duplica envíos, y notifica con evento propio.
- [ ] (E7b) Un deal con `channels: [EMAIL, SFTP]` en config entrega por ambos, con el mismo documento.
- [ ] (E7b) `core/scrappy/liquitech/` no existe más; la instrucción de Liquitech sale por config.
- [ ] (E7b) Skandia onboardeado con SFTP sin código nuevo.

### Riesgos

- El cifrado PGP de los archivos PAB (`PostDistribution.kt:470-485`) tiene que sobrevivir la
  generalización — es requisito de Bancolombia, no un detalle de Liquitech.
- E7b toca el contrato de `config_json` → coordinar con el tab 5 del wizard (⬛ BO).

---

## E8 — Conciliación como gate de la distribución (caso Inklusiva)

> *"La conciliación de plataforma reporta; la de los scripts bloquea."* No es hito del brief — entra por el
> handoff de los deals scrappy ([handoff-camilo-scrappy-deals.md](handoff-camilo-scrappy-deals.md), sección Inklusiva).

**Repo:** `master-trust-servicer-api` (el gate y los avisos) · ⬛ el matching es de otros · **Tamaño: S–M**

### Estado hoy (verificado)

- **En plataforma, la conciliación solo reporta.** Master-trust calcula porcentajes sobre FKs que otro
  escribió (predicados SQL en `ConciliationRepositoryHelper.kt:26-84`; el matching real es el extractor /
  Conci Engine del backoffice, que recién nace). La conciliación **no frena nada**.
- Lo único que conecta conciliación con distribución son los **filtros binarios por tape**
  (`gatewayConciliationRequired` / `bankConciliationRequired` / `fundsTransferRequired`,
  `DistributorCalculator.kt:111-113`): un tape sin conciliar queda fuera del pool **en silencio** — no hay
  vista agregada ni aviso. Cero gate por umbral: `grep tolerance|threshold|umbral` en
  `core/distribution/` → 0 hits.
- **En los scripts de Camilo, la conciliación SÍ bloquea** (Inklusiva, verificado en el handoff): tolerancia
  del 10% — si más del 10% del tape no concilia contra gateways, **el proceso no ejecuta y notifica al
  canal**; si concilia, distribuye y manda **correo accionable a Inklusiva con los no conciliados** (el
  cliente se autogestiona: corrige y recarga en el siguiente tape) **y correo a Axial con lo distribuido**.
- **El override real es editar código:** caso documentado — 16% sin conciliar, el cliente pidió "distribuye
  lo que se pueda", y la salida fue cambiar la tolerancia de `0.1` a `0.9` **en el código y ejecutar en
  local**. Ese cableado conciliación → gate → distribución → correos no existe en plataforma.

### Qué implica

1. **Un readiness check nuevo** (se suma a los 3 del transversal): % conciliado del pool candidato contra
   una `conciliation-tolerance` en `config_json.distributablePayments`. El predicado ya existe
   (`PAYMENT_TAPE_VS_PAYMENTS`: `pt.payment_id IS NOT NULL`) — es contar sobre el pool antes de filtrar.
   Si excede la tolerancia → no se crea distribución + evento con el % y el detalle.
2. **Dos eventos de notificación reusando lo que ya existe:** (a) `CONCILIATION_BELOW_TOLERANCE` (la corrida
   frenada, al canal interno y/o al cliente); (b) el correo accionable post-corrida — **el contenido ya
   existe**: el Excel de conciliación de 3 tabs (distribuido / tape sin payment / payments sin tape) ya se
   adjunta a la instrucción vía `distribution.documents`. Es configurar destinatarios por evento, no
   construir reportes.
3. **El override deja de ser editar código:** es el `force` del pedido #5 con motivo registrado — "el
   cliente pidió distribuir lo que se pueda" pasa a ser un botón auditado, no un `0.1→0.9` en local.
4. **Fuera de alcance explícito: el matching.** El gate **lee** FKs, no los escribe. Para Inklusiva y los
   deals scrappy, quién escribe esos FKs hoy es el conciliador de `master-servicer-apps` (⬛); migrarlo es
   territorio del Conci Engine, deal por deal. Sin matching migrado, el gate no tiene qué leer — es el
   prerrequisito por deal, análogo al de E6.

### Criterios de aceptación

- [ ] Deal con tolerancia 10% y 16% del pool sin conciliar → no se crea distribución; notificación con el % real y el detalle de no conciliados.
- [ ] Mismo deal con 5% sin conciliar → distribuye; correo al cliente con los no conciliados (accionable) y correo al lender con lo distribuido.
- [ ] Override explícito (`force` + motivo) → distribuye pese al umbral y queda auditado quién y por qué.
- [ ] Deal sin `conciliation-tolerance` configurada → comportamiento actual intacto (los filtros binarios siguen igual).
- [ ] La tolerancia se cambia por config (DB), sin deploy y sin tocar código.

### Riesgos / dependencias

- **El % es tan bueno como los FKs.** Para deals cuyo matching vive en scripts no migrados, el gate leería
  0% conciliado y bloquearía siempre → opt-in por deal, solo donde el matching ya escribe en plataforma.
- Depende del `force` del pedido #5 (para el override) y de la config de notificaciones (para los correos —
  los dos eventos son bloques distintos, el atajo del #7 alcanza).

---

## E9 — Dos monedas en la misma instrucción de distribución

> Del brief Fase 2 (verde): *"Dos monedas en la misma instrucción de distribución."*

**Repo:** `master-trust-servicer-api` · **Tamaño: M** · **No es solo un gap: hoy hay un bug de plata latente.**

### Estado hoy (verificado)

```kotlin
// AssignmentsResolver.kt:193 — dentro de processAssignmentRule
val currency = paymentTapes.firstOrNull()?.currency!!
```

El assignment tiene **un solo** campo `currency` (`AssignmentsModel.kt:98`) y se llena con la moneda del
**primer tape de la lista**. El monto, en cambio, es la suma de *todos* los tapes que matchearon (`:183`).

> **Consecuencia:** si una regla matchea tapes en COP y en USD, el motor **los suma como si fueran la misma
> moneda** y estampa la del primero — que además llega en orden no determinístico. No falla, no avisa:
> produce una instrucción con un monto que no existe. Esto es un bug de corrección, no una feature faltante.

### Qué implica

1. **Guardrail primero (S, va solo y conviene ya):** si los tapes de una regla tienen más de una moneda →
   **fallar la corrida** con el detalle, en vez de sumar. Convierte un error silencioso en uno visible.
   Esto es independiente del resto del ticket y no espera nada.
2. **Soporte real (M):** agrupar por moneda dentro de la regla → un assignment por (regla, moneda).
   El modelo ya lo permite (`currency` está a nivel assignment, no a nivel distribución), así que es
   agrupación, no cambio de esquema.
3. **La instrucción:** que el documento agrupe por moneda (subtotales por moneda, no un total mezclado) —
   vive del lado de documents-api / templates.
4. **Cuentas:** una transferencia COP y una USD salen de cuentas distintas → el `fromAccount` pasa a
   depender de la moneda. Verificar si las reglas actuales lo permiten o si hace falta cuenta-por-moneda.
5. **FX explícitamente afuera:** este ticket **no** convierte monedas. Si un deal necesita distribuir en
   una moneda distinta a la del pago, eso es conversión con TRM y es otro ticket (hay cliente TRM en
   `infra/client/`, pero el motor de distribución no lo usa hoy).

### Criterios de aceptación

- [ ] Regla que matchea tapes COP + USD **sin** el soporte activado → la corrida falla con el detalle de monedas encontradas (hoy: suma silenciosa).
- [ ] Con soporte activado → dos assignments, uno por moneda, cada uno con su monto correcto.
- [ ] La instrucción muestra subtotales por moneda; ningún total mezcla monedas.
- [ ] Deal mono-moneda (todos los actuales) → assignments byte-idénticos a antes.

### Riesgos

- Cambia el número de assignments de una corrida → revisar consumidores (Excel, instrucción, webhooks,
  métricas) antes de mergear.

---

## E10 — La corrección del cliente reemplaza al error (re-carga del payment tape)

> Del brief Fase 2 (verde): *"No se distribuye y el borrower lo fixea para el siguiente día, sube el mismo
> PT pero con Owner = Santi... ¿Override? ¿Nuevo registro? Cómo hacer para que sobreescriba la fila y no
> inserte una nueva para no hacer ruido con: tienes 1 pago sin conciliar y sin distribuir (que sería falso,
> porque ese pago fue un error que ya arreglaron)."*

**Repo: `payment-data-extractor`** (⚠️ **no** es master-trust — es el único ticket de la épica que vive
afuera del motor) · **Tamaño: M** · Habilita el bucle de autogestión del cliente que E1 y E8 asumen.

### Estado hoy (verificado)

- `payment_tape` persiste con **`INSERT IGNORE`** en lotes de 1000
  (`PaymentTapeRepository.java:200`), unique key
  **`(company_id, gateway_code, contract_type, idempotency_key)`** (`:47-48`).
- La idempotency key se arma con los `domain-fields` del `keys-specification` de cada borrower
  (`KeysProcessor.java:212-229`), **por deal** — no hay una regla global sobre si el owner participa.

> **Las dos ramas son malas, y el brief lo intuyó exacto:**
>
> | Si el owner **no** está en la key | Si el owner **sí** está en la key |
> |---|---|
> | La fila corregida choca con la vieja y **`INSERT IGNORE` la descarta en silencio**. La corrección del cliente **nunca entra** — y nadie se entera | La fila corregida es una fila nueva. Entra, pero **la errada queda viva**: duplicado contable y el falso *"1 pago sin conciliar y sin distribuir"* que describe el brief |

### Qué implica

1. **Decidir la semántica de re-carga** (producto + tech): un tape re-cargado para un mismo pago es una
   **corrección**, no un pago nuevo. Eso pide `ON DUPLICATE KEY UPDATE` sobre los campos corregibles —
   que es **el patrón que `funds_transfers` ya usa** (`FundTransferRepository.java:106-108`), mientras
   `payment_tape` usa el opuesto. Alinear los dos es el fix de raíz.
2. **Qué se puede sobreescribir y qué no:** owner y montos sí; un tape **ya distribuido** (con
   `distribution_id`) no — ahí la corrección tiene que ser un ajuste, no un update silencioso. Ese guard
   es obligatorio: sin él, un update puede cambiar plata ya girada.
3. **Trazabilidad:** que se vea que la fila fue corregida (quién/cuándo/valor anterior), o la conciliación
   pierde la explicación de por qué un pago cambió de owner.
4. **El owner en la key: dejarlo afuera.** Si el owner no participa de la key, la corrección es un update
   de la misma fila — que es exactamente lo que se busca. Meterlo en la key garantiza el duplicado.

### Criterios de aceptación

- [ ] Tape con owner errado, no distribuido, re-cargado con el owner corregido → **la misma fila** queda con el owner nuevo; no hay fila nueva ni descarte silencioso.
- [ ] El pago corregido deja de aparecer como "sin conciliar / sin distribuir" (el falso positivo del brief desaparece).
- [ ] Tape **ya distribuido** re-cargado → **no** se sobreescribe; se rechaza con motivo explícito.
- [ ] Queda registro de la corrección (valor anterior + timestamp).
- [ ] Re-procesar un archivo sin cambios sigue siendo idempotente (no genera updates espurios).

### Riesgos

- **Es el ticket más delicado de la épica:** cambia el comportamiento de escritura de la tabla central de
  Payments, para *todos* los borrowers a la vez. Requiere el guard de "ya distribuido" y test de regresión
  por cliente antes de prod.
- Toca el prerrequisito de la DB de Payments (mismo repo, misma tabla) → coordinar con ese trabajo.

---

## E11 — Correos e instrucción editables sin tech

> Del brief Fase 2 (verde): *"Editar el asunto del correo · Editar el cuerpo del correo · Adjuntos a elegir ·
> Template de adjuntos (distributed vs no distributed, etc.) · ¿Qué hacemos con el caso tipo Crediorbe,
> Somos que son instrucciones super ad-hoc?"*

**Repo:** `master-trust-servicer-api` + ⬛ **notifications-api / documents-api** · **Tamaño: M–L**
(depende de una respuesta que no tenemos)

### Estado hoy (verificado)

| Pieza | Estado |
|---|---|
| **Asunto** | ✅ ya es config, con variables: `subject` de `TransferInstructionConfig` soporta `${date}` con formato |
| **Cuerpo** | ⬛ **no está en ningún repo clonado.** Lo renderiza la notifications-api; los repos solo mandan el `context` (las variables). **Es la pregunta central y sigue abierta** |
| **Adjuntos** | ✅ la infra existe: `NotificationRequest.attachmentsUrls` + `DistributionNotificationManager.kt:34-35` adjunta los `distribution.documents` como URLs prefirmadas de S3. Lo que **no** hay es que el deal **elija** cuáles |
| **Templates de adjuntos** | Parcial: la tabla `template` (documents-api) es editable por `/templates` sin deploy; el Excel 3-tabs (distribuido / tape sin payment / payments sin tape) ya se genera |
| **Casos ad-hoc (Crediorbe, Somos)** | Crediorbe tiene `template_id` propio en `business-prod.yml:9-24` (config, no código). Somos está `# DISTRI X SCRAPPER` — su instrucción **no sale de este repo** |

### Qué implica

1. **Adjuntos configurables (S–M, lo único enteramente nuestro):** lista en config —
   `attachments: [{type, format}]` — para que el deal elija qué documentos van. La entrega ya funciona;
   es exponer la selección.
2. **Cuerpo editable:** ⬛ **pregunta bloqueante a notifications-api:** ¿los templates son editables sin
   deploy y por quién? Si sí → este ítem es *config del template*, no desarrollo nuestro (0 código acá).
   Si no → hay que decidir si el cuerpo migra a la tabla `template` del documents-api, que **ya es
   editable**. **No estimar hasta tener la respuesta.**
3. **Los ad-hoc:** el patrón Crediorbe (un `template_id` por deal) **ya resuelve el caso** sin código.
   La recomendación es empujar los ad-hoc a ese patrón en vez de crear un mecanismo nuevo. Para Somos:
   mientras su distribución la haga el scraper externo, su instrucción está fuera de alcance.

### Criterios de aceptación

- [ ] Un deal elige sus adjuntos por config y la instrucción llega con exactamente esos.
- [ ] El asunto se cambia sin deploy (ya funciona — dejar test de regresión).
- [ ] Respuesta documentada de notifications-api sobre editabilidad del cuerpo, con la decisión tomada.
- [ ] Un caso ad-hoc nuevo se resuelve con `template_id` propio, sin PR.

### Riesgos

- El ítem del cuerpo puede ser **0 trabajo nuestro o un proyecto entero** según la respuesta ⬛. Es la
  mayor incertidumbre de estimación de la épica: separar el ticket de adjuntos (accionable ya) del de
  cuerpo (bloqueado) para no quedar frenados.

---

## Trazabilidad — brief Fase 2 (lo resaltado en verde) → tickets

Extraído del PDF `Distri Engine Brief` (44 rectángulos de resaltado, `#94c47d`). Todo lo verde tiene destino:

| Ítem verde del brief | Ticket | Nota |
|---|---|---|
| Conciliación: *"cualquier lógica custom entre las tablas"* | ⬛ **fuera de alcance** | Es el **Conci Engine** (backoffice), no master-trust. Acá solo se **lee** el resultado (E8) |
| Validación owner PT vs API → frenar todo / frenar el pago | **E1** | El brief pide 2 estrategias; E1 trae 4 (agrega `API_WINS` / `TAPE_WINS`) |
| Pagos contables (bajan OPB sin caja), distribuir o no **según lender/borrower** | **E5** | El criterio lender/borrower se expresa con el `criteria` por owner que el motor **ya tiene** — no requiere mecanismo nuevo |
| Cascada de pagos: remanente de saldos bancarios | **E4** (+ depende de **E3**) | Precedente: Sistecredito ya manda remanente al borrower, hardcodeado en su adapter — generalizar eso |
| Pagos desde el master **recurrentes o one-time** | **E4 ampliado** | Hoy la deducción corre por distribución. "Recurrente" (mensual, sin corrida) es un disparador nuevo → se apoya en el pedido #4 (trigger por evento) |
| Separar pool de asignación (PT / saldo / **data source aggregation**) | **E3** | ⚠️ **Este ítem NO está en verde en el PDF** y sin embargo no existe en el producto. Confirmar con el autor si es omisión del resaltado |
| Virtual columns (capital+interés al lender, fee al borrower) | **E6** | El ejemplo del brief ($100 = 80+10+10 → $90/$10) es exactamente el caso de dos reglas con distinto `amountField` |
| **Dos monedas en la misma instrucción** | **E9** ⬅ nuevo | Y encontramos un bug de plata latente, ver E9 |
| **Ownership: re-carga del PT corregido, override vs fila nueva** | **E10** ⬅ nuevo | Vive en el **extractor**, no en el motor |
| **Notifications: cuerpo, adjuntos, templates, ad-hoc** | **E11** ⬅ nuevo | El asunto ya es config; el cuerpo es ⬛ |
| Frequency · Owner + fallback · From/To account · Balance rules · Instrucción (correos/asunto) · Notificaciones (eventos, channel IDs) | — | **No están en verde: ya existen.** Verificado en esta sesión |

---
## Transversal — Readiness checks

**Repo:** `master-trust-servicer-api` · **Tamaño: S por check** · Es la segunda pata del "validar antes de
mover plata" de la Parte 1 (la primera es E1) — no es un octavo frente, es cómo se cumple el frente 1 y el
"Resultado" del resumen.

Hoy lo único que se valida antes de correr es la frecuencia (`DistributionFrequencyChecker`: día hábil,
día de semana, ocurrencia). No se valida que haya tapes, que no haya doble corrida, ni que las cuentas de
las reglas existan — hoy una regla con cuenta inexistente **se descarta en silencio**
(`AssignmentsResolver.kt:245`).

Orden de construcción (cada uno entregable solo):
1. **"¿Ya distribuí hoy?"** — es el guard que el pedido #5 (botón `force` de OPS) necesita; mismo código, dos pedidos.
2. Pool no vacío (0 tapes → alerta, no corrida silenciosa).
3. Cuentas de todas las reglas existen (matar el descarte silencioso de `:245`).

(No hay check de ownership: E1 es Ola 1 y estos checks son Ola 2 — un check de "contar UNKNOWN" nacería
ya reemplazado por E1.)

### Criterios de aceptación

- [ ] (1) Segunda corrida del mismo borrower el mismo día → rechazada con 409/notificación, salvo override explícito.
- [ ] (2) Corrida sin tapes en el pool → no crea distribución vacía; alerta con la razón.
- [ ] (3) Regla apuntando a cuenta inexistente → la corrida falla **antes de mover plata** diciendo qué cuenta falta (hoy: descarte silencioso).

---

## Validación ponytail de la épica (2026-08-02)

Cada ticket re-auditado con la escalera: ¿tiene que existir? → ¿reusa lo que ya está? → ¿alcanza con menos?

| Ítem | Veredicto | Nota |
|---|---|---|
| Parte 1 | ✅ | 11 frentes = 11 tickets, sin promesas que la Parte 2 no respalde |
| Prerrequisito DB | ✅ | Correctamente FUERA de la épica (ya especificado en implicancias #2/#1); solo E6 lo espera |
| E1 | ✅ mínimo | Las 4 estrategias vienen pedidas por el brief (no se re-discuten); todas caben en un `when`; `BLOCK_PAYMENT` reusa la partición existente; el `!!` es fix de una línea en la raíz |
| E2 | ✅ mínimo | Swap a método que ya existe; 48h fijo (constante, no config); S3 no se construye |
| E3 | ✅ mínimo | Enum de 2 valores con default = comportamiento actual; multi-fuente fuera hasta que un deal la pida |
| E4 | ✂️ afinado | `deductions[]` como campo ADITIVO en vez de mutar `reserveAmount` — un solo cambio de contrato, Wimo intacto. Sin entidad Fee (el assignment es el tracking) |
| E5 | ✅ mínimo | Config sobre filtros existentes, posiblemente 0 código; (b) fuera hasta definir dueño del OPB |
| E6 | ✅ mínimo | Split de ~5 líneas reusando reglas/criteria; fórmulas = copiar el pipeline que ya corre; motor no-code sin ticket hasta que la pregunta 1 lo exija |
| E7 | ✅ | E7b es **deletion over addition**: borra 534 líneas y las reemplaza con config sobre infra existente |
| E8 | ✅ mínimo | Un readiness check + una tolerancia en config + 2 eventos. Reusa: el predicado de conciliación existente, el Excel 3-tabs que ya se adjunta, la config de notificaciones, y el `force` del pedido #5 como override (no se construye uno propio). El matching queda afuera (es del Conci Engine, por deal) |
| Readiness | ✂️ recortado | El check de ownership se eliminó: nacía ya reemplazado por E1 (Ola 1 vs Ola 2). Quedan 3 checks, cada uno S — más el de E8, que es el cuarto |
| E9 | ✂️ partido | El guardrail (fallar en vez de sumar mal) es **S y va solo en Ola 1** — no espera al soporte completo. `currency` ya está a nivel assignment, así que el soporte real es agrupar, no cambiar esquema. **FX afuera**: soportar dos monedas ≠ convertirlas |
| E10 | ♻️ root-cause | El fix no es un caso especial de re-carga: es alinear `payment_tape` con el patrón que **`funds_transfers` ya usa** (`ON DUPLICATE KEY UPDATE`). Un cambio en la escritura compartida, no un guard por cada caller. El owner **fuera** de la idempotency key (meterlo garantiza el duplicado) |
| E11 | ✂️ partido | Adjuntos configurables (S, nuestro, va ya) separado del cuerpo del correo (⬛ bloqueado). Los ad-hoc **no necesitan mecanismo nuevo**: el `template_id` por deal de Crediorbe ya lo resuelve |
| E4/E5 matices | ✅ sin código nuevo | El criterio lender/borrower de E5 usa el `criteria` por owner que ya existe; el remanente de E4 generaliza lo que Sistecredito ya hace. Los "recurrentes" se apoyan en el pedido #4, no en un scheduler propio |

**Lo que NO se recortó, a propósito:** las 4 estrategias de E1 (pedido explícito del brief), el guard
anti-doble y la atomicidad de E4 (caminos de plata), el fallback de E1 ante API caída (que una API externa
no frene la plata de todos), el filtro defensivo de E5, y los tests de regresión de E3/E6 (assignments
byte-idénticos, Sistecredito intacto).

## Las preguntas de producto que ordenan la épica

1. **¿Qué frente bloquea a cada deal?** (Hilco, Exitus Engen, Nissan → ¿E6? ¿E3? ¿E5?) Si ninguno necesita
   E6, el prerrequisito de DB sale del camino crítico y el desbloqueo adelanta semanas. **La pregunta más
   valiosa del documento.**
2. **E5:** ¿alcanza con "no distribuirlos" o algún deal necesita "reducir OPB"? (S vs L, un repo vs varios.)
3. **E3:** ¿algún deal necesita agregar múltiples fuentes, o a todos les alcanza pool = saldo de cuenta?
   (S vs M.)
4. **E8:** ¿para qué deals el matching ya escribe FKs en plataforma (extractor / Conci Engine) y para
   cuáles sigue en `master-servicer-apps`? El gate solo sirve donde hay FKs que leer — define qué deals
   lo estrenan y cuáles esperan su migración de matching.
5. **E11 (⬛ bloqueante de estimación):** ¿el **cuerpo** del correo es editable sin deploy en la
   notifications-api, y por quién? La respuesta mueve ese ítem entre *0 código nuestro* y *proyecto entero*.
6. **E4:** ¿un honorario recurrente puede esperar a la próxima corrida de distribución? Si sí, el
   disparador propio no se construye.

**Y una a confirmar con el autor del brief:** la sección *"Separar el pool a distribuir de la lógica de
asignación"* **no está resaltada en verde** en el PDF, pero verificamos que **no existe** en el producto
(el monto siempre sale de `sum(netAmount)`, `AssignmentsResolver.kt:183`). ¿Omisión del resaltado, o se
considera ya acordada como núcleo de la Fase 2?
