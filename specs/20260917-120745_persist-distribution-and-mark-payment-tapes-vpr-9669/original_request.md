Jira ticket: VPR-9669 (https://pmvaas1.atlassian.net/browse/VPR-9669) — "Ejecución: persistir la distribución y marcar payment tapes como distribuidos".

## Descripción original del ticket

Bloque 5 — Se ejecuta y se avisa. El paso que efectivamente mueve la plata (a nivel de registro).

Hoy (verificado): `CreateDistribution.apply(addDistribution)` — persiste `Distribution` + `Assignment` reales; luego cada payment tape distribuido se actualiza con su `distributionId` y se guarda (`PaymentTapeDataProvider.save`).

Sin config de wizard directa — este es el paso de ejecución pura, no de configuración. Es también el contrato que el backlog de unificación (VPR-9598 a VPR-9610, Oleada 1) quiere que todos los borrowers usen — hoy BIA/Finamco/Liquitech lo bypasean.

Decisión a tomar: si distri-engine-api termina siendo el motor de cómputo, confirmar si este paso de persistencia sigue viviendo en master-trust-servicer-api (que ya tiene las tablas Distribution/Assignment) o migra también — afecta directamente cómo se conectan ambos repos.

Ref: docs/proceso-distribucion-unificado.md, Sección 1 paso 10b-10c · docs/distribucion-unificada-backlog.md Oleada 1.

## Contexto conversacional y decisiones tomadas antes de escribir la historia

- Orden ya acordado con el usuario: VPR-9668 (assignments, ya implementado en 4 fases) → VPR-9669 (esta historia) → VPR-9670 (reporte).
- **Decisión de arquitectura** (tras dos rondas de investigación profunda contra `master-trust-servicer-api`): distri-engine-api escribe **directo** a las tablas `distribution`/`assignment`/`distribution_assignments` del datasource `master_trust_servicer` ya conectado — mismo patrón interno que `CreateDistribution.apply()` — en vez de llamar al endpoint HTTP público `POST /master-trusts/{id}/distributions` (que sí existe, usado por sistemas externos como scrapy-lambdas/JTP/Rapicredit/Solvento vía M2M). El usuario prefirió explícitamente el criterio interno del motor real, dado que distri-engine-api comparte datasource y está pensado para ser el motor de cómputo+persistencia, no un integrador externo.
- **Gap resuelto en VPR-9668 (Fase 4)**: no existe ningún lookup dinámico owner→cuenta en el sistema real (se configura a mano por deal). Se agregó `toAccountId` a `ComponentOwnerRule` y `accountId` a `Assignment` — todo `Assignment` que llega a esta historia ya tiene una cuenta real resuelta.
- **Alcance de esta historia — decisión explícita del usuario**: se implementan **las 2 partes** del ticket (persistir Distribution+Assignment, y marcar `payment_tape.distributionId`), aunque en el motor real son 2 mecanismos separados (`CreateDistribution.apply()` nunca toca `payment_tape`; el marcado vive en un flujo interno distinto, `DefaultDistributionCreator.createFinal()`). Esto implica una escritura nueva en el datasource de `payments_db` (hoy solo lectura para distri-engine-api).
- **`distribution.status`**: `CALCULATED` si hay al menos un `Assignment`, `NOTHING_DISTRIBUTABLE` si la lista de assignments queda vacía (mismos 2 valores verificados en el motor real, paso 8 del pipeline).
- **`distribution.first_payment_date`/`last_payment_date`**: `PoolFund` (VPR-9662/9668) hoy no lleva la fecha del payment tape que lo originó — se agrega `paymentDate` a `PoolFund` para no perder ese dato al colapsar el pool a montos; first/last se derivan como min/max sobre los funds distribuibles.
- **`assignment.currency`**: `DistributionConfigPayload.currency` (nivel deal) — no existe hoy ningún concepto de moneda más granular por regla/fund.
- **`assignment.concept`**: `ComponentOwnerRule.description` si existe; si no, el `owner`. Para el remanente (sin regla que lo reclame), el owner tal cual (el company id o el `remainingBalance.destinationAccountId`, según corresponda).
