# Original Request

**Source**: Jira ticket [VPR-9702](https://pmvaas1.atlassian.net/browse/VPR-9702) — "Lógica de cuentas: puedo transferir desde una o más cuentas hacia una o más cuentas"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1
**Parent epic**: [VPR-9698](https://pmvaas1.atlassian.net/browse/VPR-9698) — "Distribution Rules: Cascada de pagos y asignación"

## Extracted Jira description (as of 2026-08-28)

Se puede transferir el balance de cuenta de distintos owners a otras cuentas.

Definir estructura para reflejar ese matching from → (condición optional) → to

Adicionalmente tambien se pueden agregar reglas para determinar que condicion debe cumplir para
realizar la transferencia. Para eso se tiene que determinar la columna a evaluar, la condición a
cumplir y el valor.

## User additions (this session)

- Confirmado explícitamente: NO es una estructura nueva independiente — extiende
  `BalanceStrategyConfig` (VPR-9703, mismo epic, implementado en la rama base
  `feature/VPR-9703-balance-strategy`, sin mergear todavía).
- Confirmado: `condition` es UNA sola condición opcional por regla (no un builder de grupos
  OR/AND como Payment Filters) — cita del usuario: "es uno por cada regla transferencia de cuenta
  declarada".
- Confirmado: reusa directamente `PaymentFilterCondition`/`PaymentFilterOperator` ya existentes
  (VPR-9631) — sin crear modelo ni enum de condición nuevo.
- Ejemplos reales de uso dados por el usuario: "transferencias de cierto contrato" (field
  `contract_id`) o "de un determinado gatewayCode" (field `gateway_code`).
- Confirmado: `accountTransferRules` es una LISTA — permite varias combinaciones from/to/condición
  distintas bajo el mismo owner.
- Confirmado: cada cuenta se identifica por `accountId: Long`, mismo patrón que
  `AccountBalanceSource.accountId` (Pool Strategy, VPR-9629).
- Sin validación cruzada entre `fromAccountIds` y `toAccountIds` (mismo criterio permisivo del
  resto del proyecto) — no cubierto explícitamente por el usuario, asumido por consistencia con
  el resto de las decisiones de esta épica (VPR-9699, VPR-9703).
