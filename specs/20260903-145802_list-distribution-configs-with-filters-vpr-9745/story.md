**Created at**: 2026-09-03
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: —

# Story: Listar distribution configs con filtros de nombre, master trust y company

### Description
Hoy no existe ninguna forma de listar las `DistributionConfig` existentes — solo se puede consultar
una puntual por `id` o resolver la activa de una company. A medida que crecen los deals configurados,
operaciones necesita una vista de listado para encontrar configs por nombre, por master trust o por
company, sin tener que conocer el `id` de antemano. Este endpoint es la base para cualquier pantalla
de administración (backoffice) que muestre el conjunto de distribution configs de forma navegable.

### Acceptance Criteria
- [ ] **Given** configs existentes, **When** se llama a `GET /configs` sin filtros, **Then** devuelve una página de resultados (paginada) con las configs no borradas (`active = true`), en un orden estable.
- [ ] **Given** configs con distintos nombres, **When** se llama a `GET /configs?name=somos`, **Then** devuelve solo las configs cuyo `name` contiene "somos" (case-insensitive, búsqueda parcial), sin importar mayúsculas/minúsculas.
- [ ] **Given** configs de distintos master trusts, **When** se llama a `GET /configs?masterTrustId=3`, **Then** devuelve solo las configs con ese `masterTrustId` exacto.
- [ ] **Given** configs de distintas companies, **When** se llama a `GET /configs?companyId=5`, **Then** devuelve solo las configs con ese `companyId` exacto.
- [ ] **Given** varios filtros a la vez (ej. `name` + `companyId`), **When** se llama al endpoint, **Then** los filtros se combinan por AND — solo devuelve configs que cumplen todas las condiciones presentes.
- [ ] **Given** una config soft-deleted (`active = false`), **When** se llama al endpoint con cualquier combinación de filtros, **Then** esa config nunca aparece en los resultados.
- [ ] **Given** más resultados que el tamaño de página, **When** se pide `page`/`size`, **Then** la respuesta viene paginada (total de elementos, total de páginas, página actual).

### Additional Context
- Jira: VPR-9745 (sin comentarios adicionales al momento de escribir esta story).
- "Master Servicer Id" del ticket es el campo `masterTrustId` ya existente en `DistributionEngineConfigEntity` — confirmado con el usuario, no es un id de otro sistema.
- No incluye filtro por `status` (DRAFT/ACTIVE/INACTIVE) — fuera de alcance de este ticket, se puede agregar después.
