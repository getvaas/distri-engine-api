# Original Request

Jira: VPR-9663 — "Ejecución: gate de conciliación en runtime (hoy binario, evolucionar a tolerancia)".

Descripción (resumen): en `master-trust-servicer-api`, `PaymentTapeDataProvider.getPaymentTapes()`
recibe 3 flags binarios de conciliación (`withPaymentId`, `withPaymentConciliated`,
`withFundTransferId`) — un payment tape que no cumple queda fuera del pool en silencio, sin contar
el % ni tener un umbral. Alimentado por Conciliation Requirements (VPR-9633, ya definido como
requisito booleano por par de tablas en distri-engine-api) y `epica-distri-engine.md` ítem **E8**
(el gate de tolerancia con override auditado, que hoy no existe en ningún lado del código real).

El epic (E8) define esto explícitamente como **"un readiness check nuevo (se suma a los 3 del
transversal)"** — no una inferencia, es la propia definición del autor del epic. Criterios de
aceptación documentados en el epic (ninguno construido hasta esta iteración): 16% sin conciliar
sobre 10% de tolerancia bloquea con detalle; 5% bajo 10% distribuye; override auditado (`force` +
motivo) reemplaza el hack real de "editar código y correr en local" (caso Inklusiva, 16% sin
conciliar); sin `conciliation-tolerance` configurado, comportamiento binario sin cambios (opt-in).

Decisiones tomadas con el usuario antes de implementar:
1. La tolerancia configurable vive en `ConciliationRequirementsConfig` (extiende VPR-9633), no en
   un nodo nuevo.
2. Se evalúa contra tablas reales de solo lectura (mismo patrón que `payment_tape`/`distribution`),
   no delegando a un servicio externo.
3. Las columnas reales confirmadas por el usuario: `payment_id` y `fund_transfer_id` son FKs
   directas en `payment_tape` (String) — no hay que leer tablas `payments`/`funds_transfer`
   separadas. Cualquier otro par de tablas (`DISBURSEMENTS`, `BORROWER_CORE`) o regla con `gateway`
   específico no está soportado todavía.
4. El override auditado (`force` + motivo, "pedido #5" del epic) queda explícitamente fuera de
   alcance — depende de un mecanismo que no existe en ningún lado del motor todavía.

Ref: `docs/proceso-distribucion-unificado.md` Sección 1 paso 4b · `docs/epica-distri-engine.md` E8.
