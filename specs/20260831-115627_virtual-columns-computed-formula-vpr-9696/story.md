**Created at**: 2026-08-31
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9696
**Plan implemented**: @specs/20260831-115627_virtual-columns-computed-formula-vpr-9696/plan.md

# Story: Tipar Virtual Columns — columnas derivadas por fórmula sobre el payment tape

### Description
El wizard necesita poder definir columnas calculadas a partir de columnas reales del payment
tape (y de otras virtual columns ya definidas), usando una fórmula simple con operadores
aritméticos. Estas columnas después se referencian por nombre desde otras etapas (Distribution
Rules, Ownership, Payment Filters) — hoy el nodo existe solo como placeholder sin tipar.

### Acceptance Criteria
- [x] **Given** una lista de virtual columns con `name` y `formula` completos, **When** se guarda
  la config, **Then** persiste tal cual.
- [x] **Given** una virtual column sin `name` o sin `formula`, **When** se intenta guardar,
  **Then** se rechaza.
- [x] **Given** dos virtual columns con el mismo `name`, **When** se intenta guardar, **Then** se
  rechaza — el nombre es la clave con la que otras etapas la referencian, no puede repetirse.
- [x] **Given** una lista vacía o no enviada, **When** se guarda, **Then** persiste sin error — el
  deal puede no necesitar columnas calculadas.
- [x] **Given** una virtual column cuya `formula` referencia a otra virtual column ya definida
  (ej. `lender_weight` referenciando `lender_amount`), **When** se guarda, **Then** persiste sin
  error — no se valida orden de evaluación ni ciclos en este punto.

### Additional Context
`formula` se persiste como string crudo, sin validar su sintaxis en el wizard (operadores
soportados, existencia de las columnas referenciadas) — ese parseo y evaluación real por fila es
responsabilidad de la etapa de ejecución (Pista B), fuera de alcance de esta historia. Se permite
anidamiento libre entre virtual columns (una fórmula puede referenciar a otra virtual column),
consistente con el ejemplo real del mockup (`docs/distribution-engine-onboarding.html` STEP 3):

```
lender_amount = capital + interest
borrower_amount = tax + insurance + fee
lender_weight = (capital + interest) / (capital + interest + tax + insurance + fee)
```

Virtual Columns es un mecanismo **separado** de Distribution Rules (VPR-9643,
`ComponentOwnerRule`/`PaymentComponent`) — no lo reemplaza ni lo extiende. Otras etapas pueden
referenciar el `name` de una virtual column en sus campos de texto libre ya existentes (ej.
`ComponentOwnerRule.owner`, `PaymentFilterCondition.field`, `BalanceStrategyConfig.amountField`),
igual que ya referencian columnas reales del payment tape — esta historia no modifica ningún otro
nodo.

`DistributionConfigPayload.virtualColumns` pasa de `JsonNode` (placeholder) a
`VirtualColumnsConfig` tipado — no es un nodo nuevo, es la tipificación del placeholder que ya
existía en la tabla de 9 nodos.

Escenario real que motiva esta historia: BIA necesita un split ponderado por ~50-100 "kinds" de
componente — sin virtual columns, no tiene forma de entrar al wizard salvo con reglas simples de
`amountField`.
