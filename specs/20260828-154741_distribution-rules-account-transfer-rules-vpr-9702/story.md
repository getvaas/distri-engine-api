**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9702
**Plan implemented**: @specs/20260828-154741_distribution-rules-account-transfer-rules-vpr-9702/plan.md

# Story: Definir reglas de transferencia entre cuentas por balance strategy

### Description
Cuando se mueve el balance de un owner (VPR-9703), ese movimiento puede necesitar ir desde una o
más cuentas de origen hacia una o más cuentas de destino, y opcionalmente solo aplicarse si se
cumple una condición puntual (ej. solo para cierto contrato, o para un gateway determinado). El
deal necesita poder declarar esas reglas — pueden ser varias distintas bajo el mismo owner.

### Acceptance Criteria
- [x] **Given** una regla con `fromAccountIds`, `toAccountIds` y `condition` completos, **When**
  se guarda la config, **Then** persiste tal cual.
- [x] **Given** una regla sin `condition` (null), **When** se guarda, **Then** persiste sin error
  — la condición es opcional, no toda regla necesita restringirse a un caso puntual.
- [x] **Given** una lista de `accountTransferRules` vacía o no enviada, **When** se guarda,
  **Then** persiste sin error.
- [x] **Given** una regla con el mismo `accountId` presente en `fromAccountIds` y en
  `toAccountIds`, **When** se guarda, **Then** persiste sin error — no hay validación cruzada
  entre ambas listas, mismo criterio permisivo ya establecido en el proyecto.

### Additional Context
Extiende `BalanceStrategyConfig` (VPR-9703) — no es una estructura independiente. Modelo:

```java
public record AccountTransferRule(
        List<Long> fromAccountIds,
        List<Long> toAccountIds,
        PaymentFilterCondition condition
) {}
```

`BalanceStrategyConfig` gana `accountTransferRules: List<AccountTransferRule>`.

`condition` reusa directamente `PaymentFilterCondition(field, operator, value)` y
`PaymentFilterOperator` (EQ, NEQ, IN, NOT_IN, IS_NULL, IS_NOT_NULL, CONTAINS, GT, LT), ya
existentes en el proyecto desde Payment Filters (VPR-9631) — no se crea un modelo de condición
nuevo. Es UNA sola condición por regla, no un builder de grupos OR/AND. Ejemplos reales de uso:
`field=contract_id, operator=EQ, value=<id>` o `field=gateway_code, operator=EQ, value=<code>`.

Cada cuenta se identifica por `accountId: Long`, mismo patrón que `AccountBalanceSource.accountId`
de Pool Strategy (VPR-9629).

`accountTransferRules` es una lista porque bajo el mismo owner pueden convivir varias
combinaciones distintas (ej. "si contract=X mover de cuenta A a B" y "si gateway_code=Y mover de
cuenta C a D").

Es scope de configuración (Pista A) — la ejecución real de la transferencia condicional entre
cuentas es responsabilidad de la etapa de ejecución (Pista B), fuera de alcance de
`distri-engine-api`.

Parte de la épica VPR-9698 ("Distribution Rules: Cascada de pagos y asignación"), apilada sobre
VPR-9703 (Balance Strategy, implementado, sin mergear todavía).
