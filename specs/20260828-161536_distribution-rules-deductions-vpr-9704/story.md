**Created at**: 2026-08-28
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9704
**Plan implemented**: @specs/20260828-161536_distribution-rules-deductions-vpr-9704/plan.md

# Story: Definir deducciones de comisión por regla de transferencia de cuenta

### Description
Cada transferencia de cuenta declarada (VPR-9702) puede necesitar descontar una o más comisiones
antes de mover el balance — por ejemplo, una comisión de servicing que se resta del monto, o una
comisión que se transfiere a una cuenta distinta. El deal necesita poder declarar esas
deducciones: qué las identifica, cómo se calculan, a dónde va el monto descontado (si es que va a
algún lado) y con qué frecuencia se cobran.

### Acceptance Criteria
- [x] **Given** una deducción con `concept`, `type`, `value`, `accountId` y `periodicity`
  completos, **When** se guarda la config, **Then** persiste tal cual.
- [x] **Given** una deducción con `accountId=null`, **When** se guarda, **Then** persiste sin
  error — significa que la deducción no se transfiere a ninguna cuenta, simplemente reduce el
  monto.
- [x] **Given** una lista de `deductions` vacía o no enviada, **When** se guarda, **Then** persiste
  sin error — una regla de transferencia puede no tener deducciones.
- [x] **Given** varias deducciones bajo la misma regla de transferencia, **When** se guarda,
  **Then** todas persisten, sin límite de cantidad ni validación cruzada entre ellas.

### Additional Context
Extiende `AccountTransferRule` (VPR-9702) — no es una estructura independiente. Modelo:

```java
public enum DeductionType { PERCENTAGE, FIXED }
public enum DeductionPeriodicity { ALWAYS, ONCE_PER_DISTRIBUTION, ONCE_PER_MONTH, ONCE_PER_WEEK }

public record Deduction(
        String concept,
        DeductionType type,
        BigDecimal value,
        Long accountId,
        DeductionPeriodicity periodicity
) {}
```

`AccountTransferRule` gana `deductions: List<Deduction>`.

`accountId` es opcional (nullable): `null` significa que la deducción no se transfiere a ninguna
cuenta (solo reduce el monto disponible), un valor no-null significa que el monto deducido va a
esa cuenta destinataria (ej. cuenta de comisión de un servicer). Mismo patrón `Long` ya usado en
`AccountTransferRule.fromAccountIds`/`toAccountIds` y `AccountBalanceSource.accountId` (Pool
Strategy, VPR-9629).

`type` determina cómo se interpreta `value`: `PERCENTAGE` (un porcentaje del monto) o `FIXED` (un
monto fijo) — mismo patrón de campo numérico genérico ya usado en
`BalanceStrategyConfig.distributionValue` (VPR-9703), acá con 2 tipos en vez de 5.

`periodicity` declara con qué frecuencia se cobra la deducción: `ALWAYS` (cada vez que se ejecuta
la transferencia), `ONCE_PER_DISTRIBUTION`, `ONCE_PER_MONTH`, `ONCE_PER_WEEK`. El cálculo real de
si ya se cobró en el período correspondiente es responsabilidad de la etapa de ejecución (Pista
B), fuera de alcance de esta historia — acá solo se tipa y persiste el enum.

`deductions` es una lista porque una misma regla de transferencia puede tener varias deducciones
distintas (ej. una comisión de servicing fija + una comisión porcentual de originación).

Es scope de configuración (Pista A). Parte de la épica VPR-9698 ("Distribution Rules: Cascada de
pagos y asignación"), apilada sobre VPR-9702 (Account Transfer Rules, implementado, sin mergear
todavía).
