# Original Request

**Source**: Jira ticket [VPR-9704](https://pmvaas1.atlassian.net/browse/VPR-9704) — "Deductions"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1
**Parent epic**: [VPR-9698](https://pmvaas1.atlassian.net/browse/VPR-9698) — "Distribution Rules: Cascada de pagos y asignación"

> **Nota**: el conector MCP de Atlassian falló de forma sistémica durante esta sesión (error de
> proxy, reintentado varias veces sin éxito, incluso con tickets ya leídos antes). No se pudo
> fetchear el ticket real vía MCP — el texto de abajo fue pegado directamente por el usuario en el
> chat, no extraído automáticamente. Si hace falta verificar contra el ticket real más adelante,
> confirmar que coincide.

## Ticket text (pegado por el usuario, 2026-08-28)

Para cada nodo de transferencia de cuenta que se cree: from → (condicion opcional) → to.

Se puede crear una deduccion de comisiones para cada transferencia que se pretende realizar.

Cada deduccion se puede describir como:
- concepto
- tipo (porcentaje o fijo)
- valor
- account (sin movimiento, o cuenta destinatario)
- periodicidad (siempre, 1x distribucion, 1x mes, 1x semana)

## User additions (this session)

- Confirmado explícitamente: extiende `AccountTransferRule` (VPR-9702) — no es una estructura
  independiente.
- Modelo confirmado tal cual el texto del ticket, mapeado a tipos Java:
  `concept: String`, `type: DeductionType (PERCENTAGE|FIXED)`, `value: BigDecimal`,
  `accountId: Long` (nullable — "sin movimiento" = null), `periodicity: DeductionPeriodicity
  (ALWAYS|ONCE_PER_DISTRIBUTION|ONCE_PER_MONTH|ONCE_PER_WEEK)`.
- Confirmado: `deductions` es una lista (0, 1 o varias por regla de transferencia).
- Sin validaciones adicionales cubiertas explícitamente por el usuario (ej. `value` negativo,
  `concept` obligatorio) — se asume el mismo criterio permisivo ya establecido en el resto de esta
  épica (VPR-9699, VPR-9702, VPR-9703): sin validación cruzada ni de completitud al guardar.
