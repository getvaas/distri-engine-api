**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9643
**Plan implemented**: —

# Story: Distribution Rules — Owner por componente de la cuota

### Description
Algunos deals (Hilco, Nissan) necesitan repartir cada parte de la cuota — capital, interés, mora,
garantía — entre distintos dueños, en vez de tratar el pago como un monto único que va a un solo
destino. El deal necesita poder declarar, para cada componente, a quién le corresponde.

### Acceptance Criteria
- [ ] **Given** una regla `{component, owner}` por cada uno de los 4 componentes
  (`PRINCIPAL`/`INTEREST`/`LATE_FEE`/`GUARANTEE`), **When** se guarda, **Then** la config persiste
  la lista tal cual.
- [ ] **Given** una regla sin `component` o sin `owner`, **When** se intenta guardar, **Then** se
  rechaza.
- [ ] **Given** dos reglas con el mismo `component`, **When** se intenta guardar, **Then** se
  rechaza — un componente no puede tener dos owners contradictorios.
- [ ] **Given** ninguna regla configurada (lista vacía), **When** se guarda, **Then** se persiste
  vacío sin error — un deal puede no necesitar reparto por componente.

### Additional Context
Verificado contra el código real: en `SistecreditoDistributor.kt` (`master-trust-servicer-api`),
`currentGuarantee` es hoy un campo calculado que se resta como escalar único de `netAmount` — no
existe ningún split real por componente con owner asignado por separado. Los 4 componentes mapean
1:1 contra columnas reales de `PaymentTapeEntity`: `current_principal`, `current_interest`,
`moratory_interest`, `current_guarantee`.

`owner` es un identificador libre por ahora — resolverlo contra cuentas/partes reales es
responsabilidad de Ownership (VPR-9635/9636, todavía sin construir), no de esta historia. El check
de "suma de componentes ≤ netAmount" es un chequeo de ejecución (necesita montos reales en
dólares), no de configuración — queda fuera de esta historia.

Explícitamente fuera de esta primera iteración, documentados como pendientes, no bloquean esta
historia: fees/deducciones (Finkargo), multi-moneda por regla (Finkargo), regla de
remanente/cascada (Somos, Solvento, Finkargo), impuestos y seguros (sin columna propia hoy).
