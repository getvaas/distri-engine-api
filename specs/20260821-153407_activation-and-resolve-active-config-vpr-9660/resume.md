**Created at**: 2026-08-21
**Based on plan**: @specs/20260821-153407_activation-and-resolve-active-config-vpr-9660/plan.md
**Based on story**: @specs/20260821-153407_activation-and-resolve-active-config-vpr-9660/story.md

# Resume: Activar una config y resolver la activa para ejecutar

### Executive Summary
Antes de este ticket, ninguna config podía llegar a estado "activa" — el wizard permitía crear y editar,
pero no había forma de decir "esta es la que corre". Ahora existe esa transición, con la garantía de que
nunca hay dos configs activas al mismo tiempo para el mismo borrower, y el mecanismo que el motor de
ejecución va a usar para preguntar "¿cuál config uso para distribuir a este borrower hoy?".

### Technical Summary
- `ActivateDistributionConfigUseCase`: activa la config target y desactiva (a `INACTIVE`) cualquier otra
  `ACTIVE` del mismo `companyId` — cierra la decisión pendiente desde VPR-9644 de "solo una activa por
  borrower".
- `ResolveActiveDistributionConfigUseCase`: 0 activas → 404 explícito (nunca "nada que distribuir" en
  silencio); exactamente 1 → la devuelve; 2+ → 409 de invariante violada, en vez de elegir una al azar.
- Nueva query de repositorio: `findByCompanyIdAndStatusAndDeletedFalse`.
- Endpoints nuevos: `POST /configs/{id}/activate`, `GET /configs/active?companyId=`.
- 6 tests nuevos (3 de activación, 3 de resolución) — 23 tests totales en el proyecto, 0 fallas.

### Phases Completed
- [x] **Phase 1**: Activación — transición a ACTIVE con desactivación de siblings.
- [x] **Phase 2**: Resolución — los 3 escenarios (0/1/2+ activas) con su código de error correspondiente.
- [x] **Phase 3**: Tests — Mockito puro, mismo patrón que el resto del proyecto.
