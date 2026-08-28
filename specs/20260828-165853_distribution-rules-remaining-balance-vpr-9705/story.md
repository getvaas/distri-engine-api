**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9705
**Plan implemented**: @specs/20260828-165853_distribution-rules-remaining-balance-vpr-9705/plan.md

# Story: Definir destino del remanente tras aplicar todas las reglas de distribución

### Description
Después de aplicar todas las reglas de asignación, balance strategy, transferencia entre cuentas
y deducciones de un deal, puede quedar un remanente de fondos sin asignar. El deal necesita poder
declarar, opcionalmente, a qué componente de cuota se atribuye ese remanente y a qué cuenta
destino se transfiere.

### Acceptance Criteria
- [x] **Given** `remainingBalance` con `component` y `destinationAccountId` completos, **When** se
  guarda la config, **Then** persiste tal cual.
- [x] **Given** `remainingBalance` no enviado (null), **When** se guarda, **Then** persiste sin
  error — es opcional, no todo deal necesita definir qué pasa con el remanente.
- [x] **Given** `remainingBalance` con solo uno de los dos campos (`component` o
  `destinationAccountId`), **When** se guarda, **Then** persiste sin error — sin validación
  cruzada entre ambos campos.

### Additional Context
Campo global a nivel de `DistributionRulesConfig` — no vive dentro de un `ComponentOwnerRule`
puntual, porque aplica una sola vez, después de que se hayan aplicado TODAS las reglas anteriores
de la cascada (component owners, balance strategy, account transfers, deducciones). Modelo:

```java
public record RemainingBalanceConfig(
        PaymentComponent component,
        Long destinationAccountId
) {}
```

`DistributionRulesConfig` gana `remainingBalance: RemainingBalanceConfig`, opcional.

`component` reusa directamente el enum `PaymentComponent` ya existente (PRINCIPAL | INTEREST |
LATE_FEE | GUARANTEE, de VPR-9643) — no se crea un enum nuevo. `destinationAccountId: Long` sigue
el mismo patrón ya usado en toda esta épica para identificar cuentas (
`AccountTransferRule.fromAccountIds`/`toAccountIds`, `Deduction.accountId`,
`AccountBalanceSource.accountId` de Pool Strategy VPR-9629).

Es scope de configuración (Pista A) — el cálculo real de "cuánto sobra" y la transferencia
efectiva del remanente en tiempo de ejecución son responsabilidad de la etapa de ejecución (Pista
B), fuera de alcance de esta historia.

Parte de la épica VPR-9698 ("Distribution Rules: Cascada de pagos y asignación"), apilada sobre
VPR-9704 (Deductions, implementado, sin mergear todavía).
