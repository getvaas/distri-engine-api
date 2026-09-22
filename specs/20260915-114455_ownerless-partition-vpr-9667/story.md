**Created at**: 2026-09-15
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Particionar el pool entre distribuibles y ownerless

### Description
El pool ya resuelto (Bloque 2) trae cada fondo con su owner resuelto (VPR-9665), pero nada separa
todavía los que tienen owner real de los que quedaron sin resolver — necesario antes de armar
assignments (que solo deben operar sobre fondos distribuibles). Esto cierra el paso 6 del pipeline
real, con la ventaja de que acá el bug de NPE del sistema actual (`p.ownerName!!`) no puede ocurrir:
el owner nunca es `null`.

### Acceptance Criteria
- [x] **Given** un pool de fondos ya resuelto, **When** se particiona, **Then** los fondos con owner distinto de `"UNDEFINED"` quedan en `distributable` y los que tienen `"UNDEFINED"` quedan en `ownerless`.
- [x] **Given** un pool vacío, **When** se particiona, **Then** ambas listas quedan vacías, sin error.
- [x] **Given** el orquestador base (`RunDistributionUseCase`), **When** la config está lista para distribuir, **Then** el resultado incluye el pool ya particionado, no una lista plana.

### Additional Context
- Jira: VPR-9667. Referencia: `docs/proceso-distribucion-unificado.md` Sección 1 paso 6, `epica-distri-engine.md` E1.
- El fix del NPE real (`p.ownerName!!`) ya estaba resuelto de raíz desde VPR-9665 — `ResolveOwnershipUseCase` nunca devuelve `null`. Esta historia solo construye la partición en sí.
- Fuera de alcance: distinguir "ownerless por dato faltante" vs. "por mismatch no resuelto" (depende de notificaciones y del cross-check de Ownership API, ambos diferidos). El marcador `"UNKNOWN"` del sistema real no aplica acá — solo existe del lado de Ownership API, que no está implementado.
