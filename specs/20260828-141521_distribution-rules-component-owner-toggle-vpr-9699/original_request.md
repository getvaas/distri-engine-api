# Original Request

**Source**: Jira ticket [VPR-9699](https://pmvaas1.atlassian.net/browse/VPR-9699) — "Poder seleccionar si hay un owner por componente de la cuota o no"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-28)

Permitir definir un Owner para un componente de cuota.

Un componente de cuota, son un conjunto de montos que corresponde a una cuota y se puede clasificar
como: principal + interés + impuesto

Se asocia a un owner al cual se le asigna la correspondencia del monto de acuerdo a las reglas que
se apliquen.

Supongamos que tenemos una cuota total de 100$. La tabla podría confeccionarse en el mejor caso de
la siguiente manera:

| Componente cuota | Owner | Monto |
| --- | --- | --- |
| principal | GOLD GIRL | $80 |
| interés | SIMBE INVESTMENTS -Lender 2 | $10 |
| impuestos | Prestamos Villca - Borrower | $10 |

(Nota: la columna "Monto" es el resultado calculado en tiempo de distribución según las reglas —
no es un campo que el wizard configure acá. Esta historia solo cubre el toggle, no el cálculo.)

## Context from VPR-9643 (already implemented, in master)

`DistributionRulesConfig(List<ComponentOwnerRule> componentOwners)` — `ComponentOwnerRule(component:
PaymentComponent, owner: String, description: String)`. `PaymentComponent` tiene 4 valores hoy:
PRINCIPAL, INTEREST, LATE_FEE, GUARANTEE (no incluye un valor para "impuestos" — gap ya conocido,
fuera de alcance de esta historia).

## User additions (this session)

- Confirmado: se agrega el campo `hasComponentOwners: boolean` a `DistributionRulesConfig`, mismo
  patrón que `hasAccountingPayments` (VPR-9631).
- Confirmado explícitamente: NO hay validación cruzada entre `hasComponentOwners` y
  `componentOwners` al guardar. El usuario aclaró que el proyecto permite cargas parciales — la
  config se guarda igual aunque quede en un estado inconsistente, porque esa fila no está activa
  (`status != ACTIVE`). No existe hoy ningún mecanismo de validación de completitud al activar una
  config (`ActivateDistributionConfigUseCase` solo garantiza que no haya dos ACTIVE por company) —
  agregar uno queda fuera de alcance de esta historia.
- Las validaciones ya existentes de `ComponentOwnerRule` (component y owner obligatorios, sin
  componentes duplicados) no cambian.
