**Created at**: 2026-09-07
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Gate de conciliación con tolerancia configurable

### Executive Summary
El motor de ejecución ahora puede detectar, antes de distribuir, qué porcentaje del pool de payment tapes candidatos no está conciliado — y bloquear la distribución si supera una tolerancia configurable por deal, en vez del filtro binario y silencioso que usa hoy el sistema real. Esto reemplaza el hack documentado de editar código fuente y correr en local para casos como Inklusiva (16% sin conciliar).

### Technical Summary
- `ConciliationToleranceCheck` (nuevo, 4to `ReadinessCheckType.CONCILIATION_TOLERANCE`): cuenta % no conciliado sobre el pool candidato (misma ventana de días hábiles que `PaymentTapeLoadedCheck`), lo compara contra `tolerancePercentage`; `FAILED` con % y cantidad en el motivo si lo supera.
- Auto-pasa (opt-in por deal) si `pool.strategy() != PAYMENT_TAPE`, sin `tolerancePercentage` configurado, o sin grupos de reglas — sin tocar la base de datos en esos casos.
- Evalúa reglas `ConciliationRequirementsConfig` (grupos en OR, reglas en AND dentro de cada grupo) solo para 2 pares soportados: `PAYMENT_TAPE` vs `PAYMENTS` (`payment_id IS NOT NULL`) y `PAYMENT_TAPE` vs `FUNDS_TRANSFER` (`fund_transfer_id IS NOT NULL`) — cualquier otro par, o una regla con `gateway` específico, falla explícito (`UnsupportedConciliationRuleException`) en vez de evaluarse mal.
- `ConciliationRequirementsConfig` (VPR-9633) gana `tolerancePercentage` (0-100, validado); `PaymentTapeEntity` gana `paymentId`/`fundTransferId`.
- Explícitamente fuera de alcance: el override auditado `force`+motivo, notificaciones diferenciadas, y soporte para `DISBURSEMENTS`/`BORROWER_CORE`.

### Phases Completed
- [x] **Phase 1**: Config y contexto — `tolerancePercentage`, columnas nuevas en `PaymentTapeEntity`, `ReadinessCheckContext` enriquecido.
- [x] **Phase 2**: `ConciliationToleranceCheck` — implementado y testeado (13 tests: auto-pass, over/under tolerancia, OR/AND entre reglas, casos no soportados).
