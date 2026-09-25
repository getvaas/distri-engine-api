# Original Request

Ticket: VPR-9876 ("Draft Distribution", https://pmvaas1.atlassian.net/browse/VPR-9876) — sin descripción cargada en Jira.

Usuario, en `distri-engine-api`, 2026-09-25:

> "Ok, ahora si te fijaste en mastertrustservicer-api, me posicione en la rama draft-distribution-as-distribution... basicamente necesito que audites como realiza el DRAFT distribucion para ADDI. Y esa misma implementacion intentaremos reproducirlo en nuestro nuevo branch."

Después de la investigación (2 pasadas, con una corrección importante de un subagente Explore), y de discutir que el "cash release" real tiene variaciones (recálculo contra reconciliación externa) que no son solo "correr nuestro flujo de nuevo", el usuario confirmó separar en 2 tickets:

> "sabes como funciona el cash-release? tecnicamente deberia ser parecido a nuestro flujo normal, pero si vez variaciones, entonces tal vez requiere un nuevo ticket, no te parece?"

> "ok, entonces si.. Avancemos con la historia del draft" (confirmando el alcance acotado: solo el primitivo de draft distribution, sin el flujo de cash-release/reconciliación externa).

## Investigación real ya hecha (no repetir)

Contra `master-trust-servicer-api`, rama `feat/draft-distribution-as-distribution` (2 commits sobre `543160d7`, con mensajes "wip" — está a medio terminar):

- El status `DistributionStatus.draft` **ya existía antes de esta rama** (valores reales: `approved, not_enabled, cannot_distribute, nothing_distributable, draft, error`). Lo que hace esta rama es un refactor de storage (unificar tablas `draft_distribution`/`draft_assignment` en las tablas normales `distribution`/`assignment`, con un nuevo campo `AssignmentStatus` (`draft`/`finished`) por fila) — no es lógica de negocio nueva.
- **Es genérico en código, pero hoy exclusivo de ADDI en producción** — el gating vive en 2 lugares fuera de cualquier config de negocio: `deploy/terraform/main.tf:213-223` (`eventbridge-execute-addi-start-cash-release`, `borrower_code: "ADDI"`, `event_type: START_CASH_RELEASE_FROM_MASTER`) y `SyncDistributionServiceImpl.kt:~238` (`if (borrower == Borrower.ADDI) { createDraft(...) } else { distribute(...) }`).
- Ciclo real: `StartCashReleaseFromMaster.doStart()` → `DistributionRunner.createDraft()` → `DefaultDistributionCreator.createDraft()`: computa assignments igual que siempre, arma `AddDistribution.asDraft()` (`status=draft`, `generateDocuments=false`), persiste, y comitea/reserva los payment tapes inmediatamente (`PaymentTapeDataProvider.assignDistributionId`, UPDATE dirigido, no un upsert). No genera documentos ni notifica.
- Después, `FinishCashReleaseFromMaster.doFinish()` (disparado por un consumer SQS separado, alimentado por un servicio externo de reconciliación) recalcula los montos finales contra `reconciliationResults` (dato externo, contrato no visible desde este repo) vía `CalculateCashReleaseAssignment`, valida (rechaza montos negativos, rechaza que se "pierdan" assignments respecto al draft — mensajes exactos `"negative assignment amounts"`/`"lost assignments"`), y llama `DistributionRunner.approveDraft(distributionId, adjustedAssignments)` → mismo registro `Distribution` (mismo id), agrega los assignments finales (nunca borra los draft, quedan para auditoría vía `orphanRemoval=true` que solo aplica a los reads filtrados), status pasa a `approved`.
- Decisión de scope explícita del usuario: para `distri-engine-api`, en vez de reproducir el gating ad-hoc por borrower-code, se modela como un flag de config explícito por deal (config-driven, coherente con la premisa del épico). El recálculo contra `reconciliationResults` (la mitad "finish" real) queda fuera — ticket aparte.

## Contexto del repo ya relevado

- `PersistDistributionUseCase.java` (VPR-9669) hoy persiste `status` como `"CALCULATED"`/`"NOTHING_DISTRIBUTABLE"` — valores inventados en su momento, NO coinciden con los reales (`approved`/`nothing_distributable`) que ahora confirmamos. Como esta historia agrega `draft` al mismo campo, corresponde alinear los 3 valores a los reales de una vez.
- `NoDuplicateDistributionCheck` (VPR-9661) filtra por `existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween` — solo por `active`+fecha, sin mirar `status`. Una distribución DRAFT ya cuenta como "ya distribuido ese día" para este check — comportamiento aceptado tal cual, no hace falta tocarlo para esta historia.
- `RunDistributionUseCase` es el orquestador (resolve config → readiness → pool → ownership → assignments → persist → mark tapes → notify) — el modo borrador se resuelve leyendo la config ya resuelta, sin ningún endpoint/parámetro nuevo para *crear* el draft (reusa `POST /distributions/run` tal cual). Sí hace falta un endpoint nuevo para *aprobar*.
