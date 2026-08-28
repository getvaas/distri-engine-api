# Distribution Config — Estructura del `config_json`

Esta es la estructura acordada para el payload de negocio de una `DistributionConfig`, serializado
como `config_json` (columna `LONGTEXT`) sobre la entidad `distribution_engine_config`. El modelo
Java vive en `DistributionConfigPayload.java` — este doc es la referencia legible de esa misma
estructura, para no tener que releer el código o repetir la conversación cada vez.

**Convención**: Deal Info va desparramado en el root del JSON (no en su propio nodo). El resto de
las etapas del wizard vive cada una en su propio nodo (objeto anidado). Cada nodo empieza como
`JsonNode` crudo (placeholder) hasta que un ticket lo tipa — así no se pierde información entre
iteraciones ni se bloquea el avance de otras etapas.

**Este doc cubre solo Pista A (Configuración)** — los tickets listados son los que definen la
*forma* del JSON, no los que la *ejecutan*. Varias etapas tienen, además, un ticket de ejecución
separado (Pista B) que lee este JSON en runtime y hace algo real con él — esos NO están en la
tabla de abajo porque no tocan la estructura del payload. Ejemplos: Readiness Checks tiene un
motor de ejecución real (VPR-9661, `ReadinessCheckRunner`) que corre lo que VPR-9637/9638
configuran; Pool Strategy es leído por VPR-9662 (`FetchCandidatePaymentTapesUseCase`) para buscar
candidatos reales de `payment_tape`. Ver `docs/proceso-distribucion-unificado.md` para el pipeline
de ejecución completo.

## Los 9 nodos

| Nodo | Campo en `DistributionConfigPayload` | Estado | Ticket(s) — config (Pista A) |
|---|---|---|---|
| Deal Info | `country`, `currency` (root, no anidado) | ✅ Tipado | VPR-9644 |
| Pool Strategy | `pool` → `PoolConfig` | ✅ Tipado (parcial — ver abajo) | VPR-9628, VPR-9629, VPR-9630 |
| Payment Filters | `paymentFilters` → `PaymentFiltersConfig` | ✅ Tipado (completo) | VPR-9631, VPR-9632, VPR-9633, VPR-9634 |
| Virtual Columns | `virtualColumns` → `JsonNode` | ⏳ Placeholder | sin ticket de estructura aún |
| Distribution Rules | `rules` → `DistributionRulesConfig` | ✅ Tipado (scope mínimo) | VPR-9643, VPR-9699, VPR-9702, VPR-9703, VPR-9704, VPR-9705, VPR-9706 |
| Ownership | `ownership` → `OwnershipConfig` | ✅ Tipado | VPR-9635, VPR-9636 |
| Readiness Checks | `readinessChecks` → `ReadinessChecksConfig` | ✅ Tipado | VPR-9637, VPR-9638 |
| Notifications | `notifications` → `NotificationsConfig` | ✅ Tipado (`body` excluido, bloqueado) | VPR-9639, VPR-9640 |
| Transfer Instructions | `transferInstructions` → `TransferInstructionsConfig` | ✅ Tipado (scope mínimo) | VPR-9713, VPR-9714 |

## Detalle de los nodos ya tipados

### Pool Strategy (`pool`)
```
PoolConfig
├── strategy: PoolStrategyType (PAYMENT_TAPE | ACCOUNT_BALANCE | DATA_SOURCE_AGGREGATION)
├── paymentTape: PaymentTapePoolConfig (amountField, daysBack)         — VPR-9628
├── accountBalance: AccountBalancePoolConfig (accounts: [...])         — VPR-9629
└── dataSourceAggregation: JsonNode                                    — VPR-9630, placeholder
```

### Payment Filters (`paymentFilters`)
```
PaymentFiltersConfig
├── accountingPayments: AccountingPaymentsConfig                       — VPR-9631
│   (hasAccountingPayments, distributeAccountingPayments, conditionGroups: OR[AND[condición]])
├── gatewayFilters: GatewayFiltersConfig (mode, gateways: [...])       — VPR-9632
├── conciliationRequirements: ConciliationRequirementsConfig           — VPR-9633
│   (groups: OR[AND[{tableA, tableB, gateway}]])
└── dateTimeFilters: DateTimeFiltersConfig (rules: [flat, sin AND/OR]) — VPR-9634
```

### Distribution Rules (`rules`)
```
DistributionRulesConfig
├── hasComponentOwners: boolean                                        — VPR-9699
├── remainingBalance: RemainingBalanceConfig, opcional                 — VPR-9705
│   ├── component: PRINCIPAL|INTEREST|LATE_FEE|GUARANTEE
│   └── destinationAccountId: Long
└── componentOwners: [ComponentOwnerRule]
    ├── component: PRINCIPAL|INTEREST|LATE_FEE|GUARANTEE
    ├── owner: String
    ├── description: String, opcional
    ├── distributeAccountingPayments: boolean, default false             — VPR-9706
    └── balanceStrategy: BalanceStrategyConfig, opcional                — VPR-9703
        ├── amountField: String (columna libre de payment_tape, mismo patrón que
        │                 PaymentTapePoolConfig.amountField de VPR-9628)
        ├── sufficiencyStrategy: BalanceSufficiencyStrategy
        │       (SUFFICIENT_OR_STOP | UNTIL_EXHAUSTED | SKIP_IF_INSUFFICIENT | IGNORE_BALANCE)
        ├── distributionStrategy: AmountDistributionStrategy
        │       (DEFAULT | PROPORTIONAL_WEIGHT | PERCENTAGE_OF_POOL | PERCENTAGE_OF_REMAINING | FIXED_AMOUNT)
        ├── distributionValue: BigDecimal, opcional (peso/porcentaje/monto fijo según
        │           distributionStrategy; null cuando distributionStrategy=DEFAULT)
        └── accountTransferRules: [AccountTransferRule], opcional          — VPR-9702
            ├── fromAccountIds: [Long]
            ├── toAccountIds: [Long]
            ├── condition: PaymentFilterCondition, opcional (mismo modelo que Payment Filters,
        │           VPR-9631 — una sola condición por regla, no builder de grupos OR/AND)
        └── deductions: [Deduction], opcional                              — VPR-9704
            ├── concept: String
            ├── type: DeductionType (PERCENTAGE | FIXED)
            ├── value: BigDecimal
            ├── accountId: Long, opcional (null = sin movimiento, no-null = cuenta destinataria)
            └── periodicity: DeductionPeriodicity
                    (ALWAYS | ONCE_PER_DISTRIBUTION | ONCE_PER_MONTH | ONCE_PER_WEEK)
```
Scope mínimo (VPR-9643). `hasComponentOwners` declara si el deal usa esta asignación o no.
`remainingBalance` es global al deal (a diferencia de todo lo demás en este nodo) — aplica una
sola vez, después de aplicar TODAS las reglas anteriores de la cascada, al remanente sin asignar.
`balanceStrategy` vive por regla (owner+componente), no global al deal, porque cada owner puede
necesitar una estrategia distinta. `distributeAccountingPayments` es un override independiente
por componente, sin relación forzada con el flag equivalente a nivel deal
(`AccountingPaymentsConfig.distributeAccountingPayments`, VPR-9631, nodo Payment Filters) — mismo
nombre, records distintos. `accountTransferRules` es una lista porque bajo el mismo owner
pueden convivir varias combinaciones from/to/condición distintas (ej. "si contract_id=X mover de
cuenta 1 a cuenta 2" y "si gateway_code=Y mover de cuenta 3 a cuenta 4"). `deductions` declara las
comisiones a descontar de una transferencia — mismo `accountId: Long` que `fromAccountIds`/
`toAccountIds`, `null` significa que la deducción no se transfiere a ninguna cuenta. Ninguno de
estos campos tiene validación cruzada con los demás al guardar (ni `hasComponentOwners` con
`componentOwners`, ni `distributionStrategy` con `distributionValue`, ni `fromAccountIds` con
`toAccountIds`, ni entre los campos de `Deduction`) — se permiten drafts parciales/inconsistentes
mientras la config no esté `ACTIVE`, mismo criterio del resto del proyecto. El cálculo real del
monto y la resolución del
balance en tiempo de ejecución son responsabilidad de la etapa de ejecución (Pista B), fuera de
alcance de este repo. Explícitamente pendientes, sin resolver todavía: orden de la cascada de
pagos (VPR-9700), multi-moneda por regla, impuestos y seguros (no tienen columna propia hoy —
`PaymentComponent` tampoco tiene un valor para "impuestos" todavía, gap conocido).

### Ownership (`ownership`)
```
OwnershipConfig
├── source: OwnershipSourceConfig                                       — VPR-9635
│   ├── sourceType: OwnershipSourceType (OWNERSHIP_API | PAYMENT_TAPE_FIELD)
│   ├── field: String (contract_id para la API, o columna del owner en el tape —
│   │          soporta rutas como "extra_data.aux_var_3", String libre)
│   └── defaultOwner: String, opcional
└── crossValidation: OwnershipCrossValidationConfig                     — VPR-9636
    ├── enabled: boolean
    └── mismatchStrategy: OwnershipMismatchStrategy (API_WINS | TAPE_WINS | BLOCK_PAYMENT | BLOCK_DISTRIBUTION)
```
`source` y `crossValidation` son independientes y ambos opcionales. Riesgos documentados, no
resueltos: fallback si la Ownership API externa cae; que `BLOCK_PAYMENT` reuse la partición
ownerless existente es decisión de ejecución; capa de normalización/alias de owner
(Finamco/Liquitech) fuera de alcance.

### Readiness Checks (`readinessChecks`)
```
ReadinessChecksConfig
└── checks: [ReadinessCheckSetting]                                     — VPR-9637, VPR-9638
    ├── type: ReadinessCheckType (PAYMENT_TAPE_LOADED | NO_DUPLICATE_DISTRIBUTION | BUSINESS_DAY)
    ├── failureAction: ReadinessCheckFailureAction (PAUSE_AND_ALERT | SILENT_SKIP | DISTRIBUTE_PARTIALLY)
    └── retry: ReadinessCheckRetry (NO | NEXT_CYCLE | IN_1_HOUR)
```
`failureAction`/`retry` son por-check, no globales para toda la config — los borrowers reales usan
los 3 modos (Inklusiva particiona-y-sigue, Finamco bloquea-todo, Rapicredit solo-reporta), no es
válido simplificar a uno solo por default. Solo `BUSINESS_DAY` tiene un check real implementado
hoy (`BusinessDayCheck`, corrido por el motor de ejecución VPR-9661); el resto queda
`NOT_IMPLEMENTED` en runtime hasta que se construyan.

### Notifications (`notifications`)
```
NotificationsConfig
├── channels: NotificationChannelsConfig                                — VPR-9639
│   ├── channels: [NotificationChannel] (SLACK | EMAIL | WEBHOOK | ROAM)
│   └── enabledEvents: [NotificationEvent] (DISTRIBUTION_SUCCEEDED | DISTRIBUTION_FAILED |
│                        READINESS_CHECK_FAILED | OWNERLESS_PAYMENT_TAPES | TRANSFER_INSTRUCTION_READY)
├── templates: NotificationTemplatesConfig                              — VPR-9640
│   ├── subject: String
│   ├── recipients: [String] (lista plana)
│   └── documents: [DocumentTemplateRef] (name, fileName, description, format libre)
└── sftpDelivery: SftpDeliveryConfig                                    — VPR-9721
    ├── enabled: boolean
    ├── credentialKey: String        (facility_id, referencia a Secrets Manager)
    ├── remotePathTemplate: String   (soporta placeholders: {account}/{yyyy}/{MM}/{dd})
    ├── fileNameTemplate: String     (soporta placeholders)
    └── encryptionKeyRef: String?    (referencia a la key PGP, nunca en texto plano)
```
`sftpDelivery` nunca guarda credenciales ni llaves — solo referencias externas (mismo patrón que
el motor real: un Lambda compartido resuelve host/user/pass/PGP key desde AWS Secrets Manager por
`credentialKey`/`facility_id`). No es un valor más de `NotificationChannel` porque necesita una
referencia de conexión estructurada, no solo un nombre de canal.
Verificado contra el mockup real (`docs/distribution-engine-onboarding.html` STEP 7), no solo el
texto de los tickets — 2 correcciones sobre lo que el ticket sugería: `channels` mezcla canales
internos (Slack, ROAM) con el canal cliente (Email) en un solo selector plano, sin SFTP; y
`recipients` es una sola lista, no dos grupos de audiencia (cliente/lender). Riesgos documentados,
sin resolver: SFTP (E7b) sin lugar en la config; caso Inklusiva de 2 audiencias distintas.
`body` (cuerpo del mensaje) **no está modelado** — bloqueado por una pregunta sin responder sobre
notifications-api, no es una decisión de negocio nuestra.

### Transfer Instructions (`transferInstructions`)
```
TransferInstructionsConfig
└── assignments: [TransferInstructionAssignment]
    ├── ownerTemplateCode: String                                      — VPR-9713
    └── namespace: String                                              — VPR-9714 (obligatorio)
```
Cada assignment es una referencia liviana a `owner_dictionary.json` (S3, externo, variable de
entorno de infraestructura global) — el resto de los datos del owner (`owner_company_id`,
`from_account_id`, `to_account_id`, `reserve_amount`, `balance_rule`) sigue viviendo únicamente en
ese diccionario, nunca copiado acá. No se agrega `ownerCompanyId` por deal: es redundante con
`companyId`, que ya vive en la raíz de `DistributionConfig` (una distribución siempre es de una
company particular). Tampoco se agrega `masterServicerId`: ya es implícito vía `masterTrustId` en
la raíz.

`namespace` (ej. `metadata.amount`) se concatena con `ownerTemplateCode` en tiempo de distribución
para resolver metadata puntual de la instrucción — esa concatenación es responsabilidad de la
etapa de ejecución (Pista B), esta config solo tipa y persiste el string. No tiene un catálogo
cerrado de valores válidos ni restricción de unicidad — varios assignments pueden compartir el
mismo `namespace`.

La unicidad de `ownerTemplateCode` es **por registro** (mismo `distribution_engine_config`), no
global — el mismo código puede repetirse entre distintos deals sin conflicto.

Relacionado, explícitamente fuera de alcance: VPR-9715 es alta de documentos y guarda su
`templateId`, no toca esta lista.

## Endpoints por nodo

Cada nodo tipado tiene su propio `PUT` — no hay un único endpoint gigante que actualice todo el
payload de una vez (salvo `PUT /configs/{id}`, que solo cubre Deal Info):

| Endpoint | Nodo(s) que actualiza |
|---|---|
| `PUT /configs/{id}` | Deal Info |
| `PUT /configs/{id}/pool` | Pool Strategy |
| `PUT /configs/{id}/payment-filters` | Payment Filters (las 4 sub-secciones juntas) |
| `PUT /configs/{id}/distribution-rules` | Distribution Rules |
| `PUT /configs/{id}/ownership` | Ownership |
| `PUT /configs/{id}/readiness-checks` | Readiness Checks |
| `PUT /configs/{id}/notifications` | Notifications (channels/events + templates juntas) |
| `PUT /configs/{id}/transfer-instructions` | Transfer Instructions |

Virtual Columns todavía no tiene endpoint propio — se agrega cuando se tipe.

## Convención de nombres

Los nombres de campo del JSON siguen el nombre de la etapa del wizard (`paymentFilters`, no
`distributablePayments`) — renombrado explícitamente el 2026-08-24 para evitar la deriva entre el
nombre del endpoint/ticket y el nombre del campo persistido, antes de que hubiera datos reales en
producción.

## Corrección de atribución (2026-08-24)

El trabajo de config de Readiness Checks había quedado atribuido a VPR-9661 en una sesión previa.
Eso estaba mal: VPR-9661 es puramente el motor de ejecución ("corre las precondiciones
CONFIGURADAS", según su propia descripción en Jira) — la config pertenece a VPR-9637
(preconditions) y VPR-9638 (failure behavior). Al revisar VPR-9638 con más cuidado también se
encontró que el modelo implementado (un solo `failureAction`/`retry` para toda la config) no
cumplía lo que el ticket pide explícitamente ("al menos 3 modos configurables **por check**") —
se corrigió a `ReadinessCheckSetting` por-check. Este tipo de deriva (código atribuido al ticket
equivocado, o a un modelo más simple del que el ticket realmente pide) es exactamente lo que este
doc busca prevenir a futuro.
