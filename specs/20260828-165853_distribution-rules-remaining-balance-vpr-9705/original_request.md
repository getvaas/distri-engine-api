# Original Request

**Source**: Jira ticket [VPR-9705](https://pmvaas1.atlassian.net/browse/VPR-9705) — "Remaining Balance"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1
**Parent epic**: [VPR-9698](https://pmvaas1.atlassian.net/browse/VPR-9698) — "Distribution Rules: Cascada de pagos y asignación"

## Extracted Jira description (as of 2026-08-28)

Opcionalmente se puede definir un componente de cuota a transferir, sobre resto de fondos, a una
cartera destinatario luego de que se hayan aplicado todas las reglas anteriores.

## User additions (this session)

- Confirmado explícitamente: es un campo GLOBAL a nivel de `DistributionRulesConfig`, no dentro de
  un `ComponentOwnerRule` puntual — aplica una sola vez, después de aplicar TODAS las reglas
  anteriores.
- Confirmado: `component` reusa directamente el enum `PaymentComponent` ya existente (VPR-9643) —
  sin enum nuevo.
- Confirmado: `destinationAccountId: Long` sigue el mismo patrón `Long` ya usado en toda la épica
  para cuentas.
- Sin validaciones cruzadas cubiertas explícitamente — mismo criterio permisivo ya establecido en
  el resto de esta épica (VPR-9699, VPR-9702, VPR-9703, VPR-9704).
