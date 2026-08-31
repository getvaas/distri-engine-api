# Original Request

**Source**: Jira ticket [VPR-9641](https://pmvaas1.atlassian.net/browse/VPR-9641) — "Review & Activate"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-31)

Etapa 9 — Review & Activate. Confirmación final antes de activar la configuración del deal.

A diferencia de las otras 15 historias, no hay un borrower real que hoy "necesite" esta pantalla
de una forma particular — es puramente sobre el comportamiento del wizard. Pero dos decisiones de
esta etapa condicionan cómo se validan todas las demás:

**Decisiones abiertas:**

1. ¿Esta pantalla corre una simulación/dry-run contra datos históricos antes de activar? Sería el
   lugar natural para los criterios de "assignments byte-idénticos a antes" que
   `epica-distri-engine.md` exige como regresión en E3, E6 y E9 — confirmar si esa validación
   queda manual (QA en stg antes de activar) o si el wizard la ofrece como preview automático
   dentro de este paso.
2. Comportamiento al reemplazar una config existente para el mismo borrower: ¿versiona o
   sobreescribe? Es relevante para Finkargo Colombia si termina necesitando dos configuraciones
   activas a la vez (cuenta / tránsito, ver VPR-9644) — hoy no está definido si el wizard soporta
   eso o fuerza una config única por borrower.

Ref: `docs/mapeo-borrowers-configuracion.md`, Sección 4.

## User additions (this session)

- **Alcance reducido explícitamente**: esta historia cubre ÚNICAMENTE agregar la capacidad de
  desactivar una config — contraparte de `ActivateDistributionConfigUseCase`, ya implementado. Las
  2 decisiones abiertas del ticket original (dry-run, versionado) quedan explícitamente fuera,
  sin resolver.
- Confirmado: ya existe `ActivateDistributionConfigUseCase` (activa, garantiza un solo `ACTIVE`
  por `companyId`, VPR-9644) y `POST /configs/{id}/activate`.
- Modelo acordado: `DeactivateDistributionConfigUseCase.execute(String id): DistributionConfig` —
  pone status `INACTIVE`, sin validar el status actual, sin lógica de hermanos.
- Endpoint acordado: `POST /configs/{id}/deactivate`.
