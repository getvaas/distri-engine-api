**Created at**: 2026-09-07
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Gate de conciliación con tolerancia configurable

### Description
Hoy, cuando se configuran Conciliation Requirements (VPR-9633) para un deal, esa configuración
existe pero nada la evalúa contra datos reales — no hay forma de saber, antes de distribuir, qué
porcentaje del pool candidato no cumple esos requisitos. El motor real (`master-trust-servicer-api`)
filtra en silencio, sin visibilidad ni umbral, lo que en la práctica llevó a soluciones ad-hoc como
editar código fuente y correr en local (caso Inklusiva, 16% sin conciliar). Un cuarto readiness
check cierra esa brecha: cuenta el % no conciliado sobre el pool y bloquea si supera una tolerancia
configurable, dando visibilidad y control sin tocar código.

### Acceptance Criteria
- [x] **Given** un deal con `pool.strategy = PAYMENT_TAPE` y `conciliationRequirements.tolerancePercentage` configurado, **When** el % de payment tapes no conciliados en el pool candidato supera esa tolerancia, **Then** `ConciliationToleranceCheck` devuelve `FAILED` con el % y la cantidad en el motivo.
- [x] **Given** el mismo escenario pero con % no conciliado igual o por debajo de la tolerancia, **When** corre el check, **Then** devuelve `PASSED`.
- [x] **Given** un deal sin `tolerancePercentage` configurado (o sin reglas, o con `pool.strategy` distinto de `PAYMENT_TAPE`), **When** corre el check, **Then** pasa automático sin consultar nada — opt-in por deal.
- [x] **Given** una regla de conciliación con un par de tablas no soportado (cualquiera que no sea `PAYMENT_TAPE` vs `PAYMENTS` o `PAYMENT_TAPE` vs `FUNDS_TRANSFER`) o con `gateway` específico, **When** el check intenta evaluarla, **Then** falla explícito (`UnsupportedConciliationRuleException`) en vez de evaluar mal o ignorar en silencio.
- [x] **Given** varios grupos de reglas configurados (OR entre grupos, AND dentro de cada grupo), **When** un payment tape satisface al menos un grupo completo, **Then** se considera conciliado, sin importar si falla otros grupos.

### Additional Context
- Jira: VPR-9663. Referencia: `docs/epica-distri-engine.md` ítem E8, `docs/proceso-distribucion-unificado.md` Sección 1 paso 4b.
- El epic define esto como el 4to readiness check (junto a `BUSINESS_DAY`, `PAYMENT_TAPE_LOADED`, `NO_DUPLICATE_DISTRIBUTION`) — no una etapa de filtrado de pool separada.
- Explícitamente fuera de alcance: el override auditado `force`+motivo (épica "pedido #5"), las notificaciones diferenciadas (`CONCILIATION_BELOW_TOLERANCE`, cliente vs lender), y el soporte de `DISBURSEMENTS`/`BORROWER_CORE` (no hay columnas reales confirmadas para esos pares todavía).
- `payment_id` y `fund_transfer_id` son columnas String directas en `payment_tape` (confirmado por el usuario) — no hace falta mapear tablas `payments`/`funds_transfer` separadas para los 2 pares soportados.
