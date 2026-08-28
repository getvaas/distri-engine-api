# Original Request

**Source**: Jira ticket [VPR-9706](https://pmvaas1.atlassian.net/browse/VPR-9706) — "Accounting Payments: poder elegir si se quiere distribuir o no Pagos Contables (pagos sin transacción)"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1
**Parent epic**: [VPR-9698](https://pmvaas1.atlassian.net/browse/VPR-9698) — "Distribution Rules: Cascada de pagos y asignación"

## Extracted Jira description (as of 2026-08-28)

En cada componente de la cuota se puede definir si se quiere o no distribuir pagos contables.

## Context from VPR-9631 (already implemented, in master)

`AccountingPaymentsConfig(hasAccountingPayments, distributeAccountingPayments, conditionGroups)`
— nodo Payment Filters, `PaymentFiltersConfig.accountingPayments`. Ya existe
`distributeAccountingPayments: boolean` a nivel deal, decidiendo si los pagos contables
identificados por `conditionGroups` se distribuyen igual.

## User additions (this session)

- Confirmado explícitamente: es un override INDEPENDIENTE por componente — sin relación lógica
  forzada con el flag del deal. Cada uno se evalúa por separado en tiempo de ejecución (Pista B).
- Confirmado: mismo nombre de campo `distributeAccountingPayments`, pero en records distintos
  (`AccountingPaymentsConfig` vs `ComponentOwnerRule`) — sin colisión real en Java.
- Confirmado: `boolean` primitivo (no `Boolean` nullable), mismo patrón que `hasComponentOwners`
  (VPR-9699).
- Confirmado: default `false` cuando no se envía — no distribuir por defecto, opt-in explícito por
  componente.
