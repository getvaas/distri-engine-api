# Original Request

Jira: VPR-9665 — "Ejecución: resolución de ownership por payment tape" (Bloque 3 del pipeline).

Descripción: en `master-trust-servicer-api`, `OwnerNameResolverProvider.getResolverForConfig(config,
distributablePayments)` corre antes de cualquier cross-check; hoy solo existe un resolver activo
por config (Payment Tape o Ownership API), sin el cross-check ni las excepciones diseñadas en
VPR-9636. Alimentado por Ownership: Source (VPR-9635), Cross-Validation (VPR-9636).

**Decisiones del ticket:**
1. VPR-9636 (cross-check + estrategias de mismatch + exceptions) es una capacidad que no existe en
   el runtime real hoy — esta historia es donde se implementa, no solo se configura.
2. Los 3 resolvers adicionales anotados para "próxima iteración" en VPR-9635 (Somos, Rapicredit,
   Finamco) quedan fuera de esta historia también en el motor de ejecución.

**Reajuste de alcance durante el análisis** (con el usuario, tras investigar código real): la
Ownership API real requiere 2 saltos externos encadenados — `OwnerNameResolverProviderByOwnershipApi`
usa `OwnerAtomHttpClient` (contract_id → owner en Atom) y luego `CompanyClient` (owner → company) —
ninguno de los dos existe en este repo. El usuario decidió acotar esta iteración a **solo**
`PAYMENT_TAPE_FIELD` sobre la columna real `owner_name` de `payment_tape`, dejando explícitamente
Ownership API + Atom + el cross-check real para un ticket futuro dedicado al Atom tracker.

Ref: `docs/proceso-distribucion-unificado.md`, Sección 1 paso 5a.
