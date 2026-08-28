**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9703
**Plan implemented**: @specs/20260828-150406_distribution-rules-balance-strategy-vpr-9703/plan.md

# Story: Definir estrategia de balance por regla de owner de componente

### Description
Cuando un componente de cuota (principal, interés, etc.) se le asigna a un owner (VPR-9643), el
deal necesita poder definir cómo se calcula y resuelve el movimiento de ese balance hacia la
cuenta del owner: de qué columna sale el monto, qué hacer si el balance no alcanza, y cómo se
calcula el monto exacto a distribuir. Esto puede variar owner por owner — no es una única regla
para todo el deal.

### Acceptance Criteria
- [x] **Given** una regla con `balanceStrategy` completo (`amountField`, `sufficiencyStrategy`,
  `distributionStrategy`, `distributionValue`), **When** se guarda la config, **Then** persiste
  tal cual.
- [x] **Given** una regla sin `balanceStrategy` (null), **When** se guarda la config, **Then**
  persiste sin error — no todas las reglas necesitan tener la estrategia definida todavía.
- [x] **Given** `distributionStrategy=FIXED_AMOUNT` sin `distributionValue`, **When** se guarda,
  **Then** persiste sin error — no hay validación cruzada entre ambos campos (mismo criterio que
  VPR-9699: se permiten drafts parciales/inconsistentes).
- [x] **Given** `distributionStrategy=DEFAULT` con `distributionValue` seteado igual, **When** se
  guarda, **Then** persiste tal cual, sin rechazar el dato "de más".

### Additional Context
Modelo acordado — vive por regla (`ComponentOwnerRule`), no global al deal, porque cada
owner/componente puede necesitar una estrategia distinta:

```java
public enum BalanceSufficiencyStrategy { SUFFICIENT_OR_STOP, UNTIL_EXHAUSTED, SKIP_IF_INSUFFICIENT, IGNORE_BALANCE }
public enum AmountDistributionStrategy { DEFAULT, PROPORTIONAL_WEIGHT, PERCENTAGE_OF_POOL, PERCENTAGE_OF_REMAINING, FIXED_AMOUNT }

public record BalanceStrategyConfig(
        String amountField,
        BalanceSufficiencyStrategy sufficiencyStrategy,
        AmountDistributionStrategy distributionStrategy,
        BigDecimal distributionValue
) {}
```

`ComponentOwnerRule` gana `balanceStrategy: BalanceStrategyConfig`, opcional.

Significado de cada valor (según el ticket):
- `SUFFICIENT_OR_STOP`: solo distribuye si el balance alcanza para el monto completo; si no, no
  distribuye nada.
- `UNTIL_EXHAUSTED`: distribuye lo que se pueda hasta agotar el balance disponible (parcial
  permitido).
- `SKIP_IF_INSUFFICIENT`: si no alcanza, saltea ese owner/componente este ciclo, sin bloquear a
  los demás.
- `IGNORE_BALANCE`: distribuye igual, sin chequear si el balance alcanza.
- `DEFAULT`: suma la columna de monto agrupada por owner, sin cálculo especial —
  `distributionValue` queda `null` en este caso.
- `PROPORTIONAL_WEIGHT` / `PERCENTAGE_OF_POOL` / `PERCENTAGE_OF_REMAINING` / `FIXED_AMOUNT`: cada
  uno usa `distributionValue` con un significado distinto (peso/porcentaje/monto fijo
  respectivamente) — es el mismo campo numérico genérico, no se separan en 3 campos.

`amountField` sigue el mismo patrón ya usado en el proyecto (`PaymentTapePoolConfig.amountField`,
VPR-9628): string libre, cualquier columna real de `payment_tape`, no un enum cerrado.

Esta historia es puramente de configuración (Pista A) — el cálculo real del monto a distribuir y
la resolución del balance en tiempo de ejecución son responsabilidad de la etapa de ejecución
(Pista B), fuera de alcance de `distri-engine-api`.

Es parte de la épica VPR-9698 ("Distribution Rules: Cascada de pagos y asignación"), junto a
VPR-9699 (toggle `hasComponentOwners`, ya implementado, sin mergear todavía) y VPR-9700 a VPR-9707
(pendientes, fuera de esta historia).
