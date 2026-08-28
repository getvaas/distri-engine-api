# Original Request

**Source**: Jira ticket [VPR-9703](https://pmvaas1.atlassian.net/browse/VPR-9703) — "Balance Strategy"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1
**Parent epic**: [VPR-9698](https://pmvaas1.atlassian.net/browse/VPR-9698) — "Distribution Rules: Cascada de pagos y asignación"

## Extracted Jira description (as of 2026-08-28)

Balance strategy define la estrategia para realizar el movimiento de fondos de balance de un
owner a otra cuenta.

Principalmente se define a partir de que columna de monto se va a realizar esa estrategia y se
selecciona el tipo de estrategia. Los tipos de estrategias que se pueden realizar son:

* Sufficient or stop
* Until exhausted
* Skip in insufficient
* Ignore balance

Tambien se selecciona la estrategia de monto a distribuir:

* Default - (SUM amount column by owner)
* Proportional weight
* Percentage of pool
* Percentage of remaining
* Fixed amount

## Sibling subtasks under VPR-9698 (context, not this scope)

VPR-9699 (hasComponentOwners toggle, implementado), VPR-9700 (Orden de la cascada de pagos),
VPR-9701 (Creación de Assignment, ON HOLD), VPR-9702 (Lógica de cuentas: transferir desde una o
más cuentas hacia una o más cuentas), VPR-9704 (Deductions), VPR-9705 (Remaining Balance), VPR-9706
(Accounting Payments toggle), VPR-9707 (Restante a distribuir).

## User additions (this session)

- Confirmado: Balance Strategy vive POR `ComponentOwnerRule` (una estrategia por component+owner),
  no global a nivel de `DistributionRulesConfig`.
- Confirmado: se captura tanto el enum de estrategia como el parámetro numérico
  (`distributionValue`) — sin el número, 4 de las 5 estrategias de distribución no sirven para
  nada real.
- Confirmado el significado de cada valor de los 2 enums, explicado en la conversación y volcado
  en `story.md`.
- Confirmado: un solo campo `distributionValue: BigDecimal` genérico, reusado con distinto
  significado según `distributionStrategy` — no 3 campos separados (weight/percentage/amount).
- Confirmado: `balanceStrategy` es opcional en `ComponentOwnerRule` (puede ser `null`).
- Confirmado: sin validación cruzada entre `distributionStrategy` y `distributionValue` — mismo
  criterio ya establecido en VPR-9699 de este proyecto (se permiten drafts parciales).
- `amountField` sigue el patrón ya usado en `PaymentTapePoolConfig.amountField` (VPR-9628): string
  libre.
