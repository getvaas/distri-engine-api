**Created at**: 2026-08-21
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9628
**Plan implemented**: @specs/20260821-152719_pool-strategy-payment-tape-vpr-9628/plan.md

# Story: Pool Strategy — Payment Tape

### Description
La distribución necesita saber de dónde sale el monto a repartir. El caso default (y el único que hoy
usan la mayoría de los borrowers) es sumar los payment tapes no distribuidos de una ventana de días. El
deal tiene que poder elegir qué columna de `payment_tape` usar como monto y cuántos días hacia atrás
buscar, sin tocar código.

### Acceptance Criteria
- [x] **Given** una config sin Pool Strategy configurado, **When** se guarda sin especificar nada,
  **Then** usa `PAYMENT_TAPE` + `net_amount` + 90 días por default.
- [x] **Given** un deal que necesita otra columna (`gross_amount` u otra columna real), **When** se
  especifica `amountField`, **Then** se persiste tal cual, sin restringirlo a un enum cerrado.
- [x] **Given** `daysBack` negativo, **When** se intenta guardar, **Then** se rechaza con un error
  explícito.
- [x] **Given** una config con Deal Info ya definido, **When** se configura Pool Strategy, **Then** el
  país/moneda/nombre no se pierden.

### Additional Context
`ACCOUNT_BALANCE` (VPR-9629) y `DATA_SOURCE_AGGREGATION` (VPR-9630) se pueden seleccionar como
`strategy`, pero su configuración específica queda como placeholder hasta sus propios tickets — mismo
patrón incremental que el resto del payload (ver VPR-9644).
