**Created at**: 2026-08-21
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9660
**Plan implemented**: @specs/20260821-153407_activation-and-resolve-active-config-vpr-9660/plan.md

# Story: Activar una config y resolver la activa para ejecutar

### Description
El motor de ejecución necesita saber, dado un borrower, cuál es su configuración vigente para distribuir
— es el primer paso real del pipeline (Bloque 1: "arranca la corrida"). Hoy no existe ningún mecanismo
para que una config pase de `DRAFT` a `ACTIVE`, así que "resolver la config activa" no tiene todavía
nada real que resolver. Este ticket cierra ese hueco: agrega la activación (con la regla de "una sola
activa por borrower" que ya habíamos decidido en Deal Info, VPR-9644, pero nunca implementamos) y el
mecanismo de resolución que el motor de ejecución va a usar para arrancar una corrida.

### Acceptance Criteria
- [x] **Given** una config en `DRAFT`, **When** se activa, **Then** pasa a `ACTIVE`.
- [x] **Given** un borrower con una config ya `ACTIVE`, **When** se activa una config distinta del mismo
  borrower, **Then** la anterior pasa a `INACTIVE` — nunca hay dos activas al mismo tiempo.
- [x] **Given** un borrower con exactamente una config `ACTIVE`, **When** se pide resolver la activa por
  `companyId`, **Then** se devuelve esa config.
- [x] **Given** un borrower sin ninguna config `ACTIVE`, **When** se pide resolver la activa, **Then**
  falla con un error explícito (404) — no debe interpretarse como "nada que distribuir" en silencio.
- [x] **Given** (defensivo) más de una config `ACTIVE` para el mismo borrower, **When** se pide resolver,
  **Then** falla con un error de invariante (409) en vez de elegir una al azar.

### Additional Context
Esto no es todavía el trigger de ejecución completo (BPM vs. endpoint propio queda para cuando el resto
del pipeline de ejecución esté armado) — es específicamente el mecanismo de activación + resolución que
cualquier trigger futuro va a necesitar.
