**Created at**: 2026-08-21
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9661
**Plan implemented**: @specs/20260821-154114_readiness-checks-engine-business-day-vpr-9661/plan.md

# Story: Motor de Readiness Checks + check de día hábil

### Description
Antes de distribuir, el motor tiene que validar precondiciones configurables (Payment tape cargado, No
duplicate distribution, Día hábil) y saber qué hacer si alguna falla (pausar y alertar, saltear en
silencio, distribuir parcial — con o sin reintento). Hoy nada de esto existe en `distri-engine-api`. De
los 3 checks, solo "Día hábil" es implementable sin depender de otra capacidad (payment tape fetching,
historial de ejecuciones) — los otros dos quedan explícitamente pendientes, marcados como tales en
tiempo de ejecución en vez de simulados.

### Acceptance Criteria
- [x] **Given** una config sin Readiness Checks configurado, **When** se guarda sin especificar nada,
  **Then** usa los 3 checks habilitados + `PAUSE_AND_ALERT` + reintento `NEXT_CYCLE` por default (mismo
  default que el mockup).
- [x] **Given** un `companyId` con config activa y una fecha que cae en fin de semana, **When** se corren
  los checks, **Then** el check de día hábil falla y `readyToDistribute=false`.
- [x] **Given** la misma config y una fecha entre semana, **When** se corren los checks, **Then** el
  check de día hábil pasa.
- [x] **Given** un check habilitado sin implementación real todavía (Payment tape cargado, No duplicate
  distribution), **When** se corren los checks, **Then** se marca `NOT_IMPLEMENTED` explícitamente — no
  se ignora en silencio ni se asume que pasó.

### Additional Context
El motor de checks (`ReadinessCheckRunner`) queda diseñado para que agregar los 2 checks que faltan sea
solo registrar un nuevo bean — no hace falta tocar el runner cuando se implementen (payment tape fetching
es VPR-9662+, historial de ejecuciones todavía no tiene ticket). El calendario de feriados por país
tampoco existe aún — el check de día hábil hoy solo valida fin de semana.
