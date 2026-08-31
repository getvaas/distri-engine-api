**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9706
**Plan implemented**: @specs/20260828-172628_distribution-rules-accounting-payments-per-component-vpr-9706/plan.md

# Story: Habilitar distribución de pagos contables por componente de cuota

### Description
El deal ya puede decidir, a nivel general, si distribuye pagos contables (VPR-9631). Ahora
necesita poder afinar esa decisión componente por componente: un owner de un componente puntual
(ej. late fee) puede querer excluir o incluir pagos contables de forma independiente al resto del
deal.

### Acceptance Criteria
- [x] **Given** una regla con `distributeAccountingPayments=true`, **When** se guarda la config,
  **Then** persiste tal cual.
- [x] **Given** una regla con `distributeAccountingPayments=false`, **When** se guarda, **Then**
  persiste tal cual.
- [x] **Given** una regla sin `distributeAccountingPayments` enviado, **When** se guarda, **Then**
  persiste como `false` — mismo patrón que `hasComponentOwners` (VPR-9699).
- [x] **Given** el flag a nivel deal (`AccountingPaymentsConfig.distributeAccountingPayments`,
  VPR-9631) en cualquier valor, **When** se guarda una regla con su propio
  `distributeAccountingPayments`, **Then** ambos persisten de forma independiente, sin validación
  cruzada entre ellos.

### Additional Context
`ComponentOwnerRule` gana `distributeAccountingPayments: boolean`. Es un override **independiente
por componente** — no tiene relación lógica forzada con el flag equivalente a nivel deal
(`AccountingPaymentsConfig.distributeAccountingPayments`, VPR-9631, nodo Payment Filters). Ambos
comparten nombre de campo por describir el mismo concepto de negocio, pero viven en records
distintos y se evalúan por separado en tiempo de ejecución (Pista B) — no hay colisión ni
dependencia en el modelo de esta historia.

Es scope de configuración (Pista A) — el uso real de este flag durante la distribución (filtrar o
no pagos contables al calcular el monto de ese componente específico) es responsabilidad de la
etapa de ejecución, fuera de alcance de esta historia.

Parte de la épica VPR-9698 ("Distribution Rules: Cascada de pagos y asignación"), creada desde
`master` actualizado (ya incluye VPR-9699 a VPR-9705 mergeados).
