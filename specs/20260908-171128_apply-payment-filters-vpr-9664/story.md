**Created at**: 2026-09-08
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Aplicar Payment Filters sobre el pool candidato

### Description
Las 3 sub-configs de Payment Filters (Accounting Payments, Gateway Filters, Date & Time Filters)
ya existían pero nada las evaluaba contra datos reales — un deal podía configurarlas sin que
tuvieran ningún efecto en qué payment tapes terminan en el pool. Esto cierra el "Bloque 2" completo
del pipeline real: ventana + query (VPR-9662), gate de conciliación (VPR-9663), y ahora los 3
Payment Filters aplicados en secuencia sobre lo que sobrevivió el gate.

### Acceptance Criteria
- [x] **Given** un deal con Accounting Payments habilitado (`hasAccountingPayments=true`) y `distributeAccountingPayments=false`, **When** un payment tape matchea alguno de los grupos de condiciones configurados, **Then** ese tape queda excluido del pool.
- [x] **Given** el mismo escenario pero con `distributeAccountingPayments=true`, **When** el tape matchea, **Then** se mantiene en el pool igual.
- [x] **Given** Gateway Filters en `INCLUDE_ONLY`/`EXCLUDE`, **When** se evalúa el pool, **Then** solo sobreviven (o se excluyen) los tapes cuyo `gateway_code` está en la lista configurada.
- [x] **Given** reglas de Date & Time Filters (`DISTRIBUTE_BY_DATE`/`DISTRIBUTE_BY_DATE_TIME`/`DAYS_BACK_LIMIT`), **When** se evalúan contra la fecha de ejecución, **Then** cada regla filtra correctamente, respetando `gateway` (si está seteado, solo aplica a esa gateway), `cutoffTime` (afina la comparación por hora) y `businessDays` (días hábiles vs. corridos).
- [x] **Given** una condición de Accounting Payments sobre un campo no mapeado en `payment_tape`, o un operador `GT`/`LT` sobre un campo no numérico, **When** se evalúa, **Then** falla explícito en vez de evaluar mal o ignorar en silencio.
- [x] **Given** los 3 filtros configurados a la vez, **When** se resuelve el pool candidato, **Then** se aplican en secuencia — un tape debe sobrevivir los 3 para seguir en el pool.

### Additional Context
- Jira: VPR-9664. Referencia: `docs/proceso-distribucion-unificado.md` Sección 1 paso 4c.
- Confirmado con el usuario: las 3 sub-configs ya construidas (VPR-9631/9632/9634) mapean 1:1 contra el enum real `DistributionFilterType` — no hizo falta restructurar el modelo.
- `gateway_code` (columna real de `payment_tape`, confirmada por el usuario) habilita tanto Gateway Filters como el `gateway` scoping de Date & Time Filters.
- El "Days back" de Pool Strategy (VPR-9628, ya usado para acotar la query SQL) y `DAYS_BACK_LIMIT` de Date & Time Filters componen naturalmente: Pool Strategy determina la ventana que se trae de la DB, `DAYS_BACK_LIMIT` solo puede acotar más, nunca ampliar.
- Fuera de alcance: soporte para columnas de `payment_tape` no mapeadas todavía en las condiciones de Accounting Payments (mismo criterio incremental que `amountField`).
