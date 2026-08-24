# Original Request

**Source**: Jira ticket [VPR-9643](https://pmvaas1.atlassian.net/browse/VPR-9643) — "Distribution rules: owner por componente de la cuota"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-24, post-refinamiento)

Reglas de asignación — a quién va cada parte de la cuota (capital, interés, mora, garantía). Es E6
de epica-distri-engine.md. Spec detallada en un Google Sheet vinculado (no leído en esta sesión —
ver "User additions" abajo).

Escenarios reales: Sistecredito (`currentGuarantee` load-bearing hoy, se resta del `netAmount`),
Hilco/Nissan (capital al fondeador, impuestos/seguros a quien deba pagarlos — el caso que motiva
E6 completo), y un check obligatorio de suma-de-componentes ≤ `netAmount`.

**Decisiones abiertas originales** (4 puntos — ver resolución de scope abajo):
1. Fees/deducciones (Finkargo Colombia, E4 `deductions[]`).
2. Multi-moneda por regla (Finkargo, E9).
3. Regla de remanente/cascada (Somos, Solvento, Finkargo, E4 punto 5).
4. Impuestos y seguros sin columna propia.

Ref: `docs/mapeo-borrowers-configuracion.md` Sección 2 y 3 · `epica-distri-engine.md` E6, E4.

## User additions (this session)

- Se decidió NO autenticar Google Drive para leer el Sheet vinculado — el usuario pidió avanzar
  solo con lo que ya está en Jira + verificación directa contra el código Kotlin real, sin más
  vueltas de proceso.
- Verificación en código (`SistecreditoDistributor.kt`, `master-trust-servicer-api`): confirmado
  que `currentGuarantee` es un campo calculado, restado hoy como escalar único — no hay split real
  por componente en producción. Los 4 componentes de la épica mapean 1:1 contra columnas reales de
  `PaymentTapeEntity`: `current_principal`, `current_interest`, `moratory_interest`,
  `current_guarantee`.
- **Decisión de scope explícita del usuario**: "Quedamos en que primero íbamos a tener una
  iteración con mínimo el configurable" — los 4 puntos abiertos originales quedan expresamente
  fuera de esta historia, documentados como pendientes para historias futuras, no bloquean el
  avance de esta.
- Tipa el placeholder `rules` (hoy `JsonNode`) en `DistributionConfigPayload`. Nuevo endpoint
  `PUT /configs/{id}/distribution-rules`.
