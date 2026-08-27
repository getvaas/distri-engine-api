# Original Request

**Sources**:
- Jira ticket [VPR-9637](https://pmvaas1.atlassian.net/browse/VPR-9637) — "Readiness check: preconditions"
- Jira ticket [VPR-9638](https://pmvaas1.atlassian.net/browse/VPR-9638) — "Readiness check: failure behavior"
- Jira ticket [VPR-9661](https://pmvaas1.atlassian.net/browse/VPR-9661) — "Ejecución: motor de Readiness Checks" (solo lectura, para confirmar el deslinde)

**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## VPR-9637 — Preconditions (extracto)
Qué se valida antes de correr una distribución. Hoy solo se valida frecuencia
(`DistributionFrequencyChecker`); el transversal de la épica agrega: (1) ¿ya distribuí hoy? (2)
pool no vacío (3) cuentas de todas las reglas existen. Decisiones abiertas: frontera con Payment
Filters para checks de datos del pago, y si el guardrail de frescura de BIA entra en esta épica.

## VPR-9638 — Failure behavior (extracto)
Qué pasa cuando un check falla: bloquea, notifica, o ambos. 3 comportamientos reales distintos
conviven en producción: Inklusiva particiona-y-sigue, Finamco bloquea-todo, Rapicredit
solo-reporta (nunca bloquea). **Cita textual clave**: "Esta etapa necesita al menos 3 modos
configurables por check: bloquea-todo / particiona-y-sigue / solo-reporta — los borrowers reales
usan los 3, no es una simplificación válida quedarse con uno solo por default." Decisión abierta:
si el override auditado (`force` + motivo) vive aquí o en Payment Filters.

## VPR-9661 — Motor de ejecución (extracto, para confirmar el deslinde)
"Diferencia clave con el ticket de configuración: VPR-9637/9638 definen QUÉ checks existen y qué
hacer si fallan (config). Esta historia es el motor que los EJECUTA en runtime contra el deal
real, en el momento de la corrida." — Confirma que VPR-9661 es puramente ejecución (Pista B), no
config. No se toca en esta historia.

## User additions (this session)

- El usuario notó (correctamente) que algo estaba raro en cómo se presentaban los bloques de
  Payment Filters/Readiness Checks — pidió distinguir claramente qué es configurable (Pista A) y
  qué es ejecutable (Pista B) para poder armar entregables atómicos.
- Al investigar, se encontró que el trabajo de config de Readiness Checks de una sesión anterior
  estaba mal atribuido a VPR-9661 (que es solo ejecución) — debía estar bajo VPR-9637/9638.
- Al revisar VPR-9638 con más cuidado, se encontró que el modelo implementado (`failureAction`/
  `retry` global para toda la config) no cumple lo que el ticket pide explícitamente (modos
  configurables **por check**). El usuario eligió corregir ambos problemas: la atribución y el
  modelo.
