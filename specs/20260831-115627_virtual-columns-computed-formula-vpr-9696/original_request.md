# Original Request

**Source**: Jira ticket [VPR-9696](https://pmvaas1.atlassian.net/browse/VPR-9696) — "Virtual Columns: computed columns from Payment Tape"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-31)

Etapa 4 — Virtual Columns. Define columnas derivadas por fórmula sobre columnas del payment tape
(y sobre otras virtual columns ya definidas), evaluadas por fila antes de correr Distribution
Rules/Ownership/Payment Filters. Según el mockup (`docs/distribution-engine-onboarding.html`
STEP 3):

```
Virtual Columns: lista de {name, formula} — ej.:
  lender_amount = capital + interest
  borrower_amount = tax + insurance + fee
  lender_weight = (capital + interest) / (capital + interest + tax + insurance + fee)

Operadores soportados: + - * / y paréntesis
Puede referenciar: columnas reales del PT + otras virtual columns ya definidas
Dónde se usan después: Distribution Rules (columnas del assignment) · Proportional Weight
  (columna de peso) · Ownership (campo owner) · Payment Filters (field filters)
```

**Escenario real que motiva esta etapa**: BIA — split ponderado por ~50-100 "kinds" de
componente; sin esta etapa, BIA no tiene forma de entrar al wizard salvo con reglas simples de
`amountField`. El ejemplo del brief original ($100 = 80 capital + 10 interés + 10 fee → $90 al
lender / $10 al borrower) es exactamente un caso de virtual columns.

**Nota de scope original del ticket** (antes de esta sesión): "hoy no tiene ninguna decisión
tomada — se crea para dejar de estar sin story, no para bloquear su definición". Listaba 4
preguntas abiertas:
1. No existe motor de evaluación de fórmulas — cómo se parsea/evalúa la expresión.
2. Orden de evaluación entre virtual columns que se referencian entre sí — anidamiento libre o
   solo 1 nivel.
3. Cómo interactúa con Distribution Rules (VPR-9643, `componentOwners` sobre 4 componentes fijos
   PRINCIPAL/INTEREST/LATE_FEE/GUARANTEE).
4. Ambigüedad de atribución entre `epica-distri-engine.md` y `mapeo-borrowers-configuracion.md`
   sobre si esto es lo mismo que VPR-9643.

## User additions (this session) — las 4 preguntas de arriba, resueltas

1. **Resuelto**: `formula` se persiste como string crudo, sin validar sintaxis en el wizard — el
   parseo/evaluación real es Pista B.
2. **Resuelto**: anidamiento libre entre virtual columns, sin detección de ciclos en esta
   historia — coincide con el ejemplo real del mockup.
3. **Resuelto**: Virtual Columns y Distribution Rules son mecanismos separados que coexisten —
   otras etapas referencian el `name` de una virtual column en sus campos de texto libre ya
   existentes, sin tocar `PaymentComponent` ni agregar campos nuevos a otros nodos.
4. **Resuelto**: esta historia es la etapa propia "Virtual Columns", distinta de VPR-9643.

Modelo acordado:
```java
public record VirtualColumn(
        String name,
        String formula
) {}

public record VirtualColumnsConfig(
        List<VirtualColumn> columns
) {}
```
`DistributionConfigPayload.virtualColumns` cambia de tipo: `JsonNode` → `VirtualColumnsConfig`.

Validación: `name` y `formula` obligatorios, sin `name` duplicado dentro de `columns` — mismo
patrón ya usado en `ComponentOwnerRule`/`PaymentComponent` (VPR-9643) y
`TransferInstructionsConfig`/`ownerTemplateCode` (VPR-9713).
