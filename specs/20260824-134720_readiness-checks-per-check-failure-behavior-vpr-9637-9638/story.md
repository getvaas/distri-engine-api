**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9637 @https://pmvaas1.atlassian.net/browse/VPR-9638
**Plan implemented**: —

# Story: Readiness Checks — comportamiento de falla por check

### Description
Cada readiness check necesita su propio comportamiento cuando falla — no todos los borrowers
reaccionan igual: Inklusiva particiona y sigue, Finamco bloquea toda la corrida, Rapicredit nunca
bloquea y solo reporta. El deal necesita poder configurar, por cada check habilitado, qué pasa si
falla (y con qué reintento) — no un único comportamiento global para todos los checks a la vez.

### Acceptance Criteria
- [ ] **Given** una lista de checks habilitados con `failureAction` y `retry` propios por cada
  uno, **When** se guarda, **Then** la config persiste cada check con su propia configuración,
  independiente del resto.
- [ ] **Given** un check sin `failureAction` u sin `retry` explícito, **When** se guarda, **Then**
  usa los defaults (`PAUSE_AND_ALERT`, `NEXT_CYCLE`) para ese check puntual, sin afectar a los
  demás.
- [ ] **Given** el mismo `type` de check repetido dos veces en la lista, **When** se intenta
  guardar, **Then** se rechaza — un check no puede tener dos configuraciones contradictorias.
- [ ] **Given** ninguna lista enviada (o vacía), **When** se guarda, **Then** se habilitan los 3
  checks conocidos (`PAYMENT_TAPE_LOADED`, `NO_DUPLICATE_DISTRIBUTION`, `BUSINESS_DAY`) con los
  defaults — mismo comportamiento de "todo habilitado por default" que existía antes.
- [ ] **Given** el motor de ejecución (`ReadinessCheckRunner`, VPR-9661), **When** corre los checks
  de una config con settings por-check, **Then** sigue evaluando exactamente los mismos tipos de
  check que antes — el cambio de estructura no le rompe la lectura.

### Additional Context
**Corrección de atribución**: el trabajo de config de Readiness Checks (`UpdateReadinessChecksConfigUseCase`,
`ReadinessChecksConfig`) había quedado atribuido a VPR-9661 en sesiones previas — eso estaba mal.
VPR-9661 es puramente el motor de ejecución ("corre las precondiciones CONFIGURADAS", según su
propia descripción); la configuración pertenece a VPR-9637 (preconditions) y VPR-9638 (failure
behavior). Esta historia corrige eso y además resuelve el gap real que motivó la corrección: el
modelo anterior tenía un solo `failureAction`/`retry` global para todos los checks, pero VPR-9638
pide explícitamente al menos 3 modos configurables **por check**, no uno para toda la config.

**Explícitamente fuera de esta historia** (decisiones abiertas de VPR-9637/9638, no resueltas
aquí, no bloquean el avance):
1. La frontera exacta entre esta etapa y Payment Filters para checks que dependen de datos del
   pago (frescura, conciliación) vs. estado de la corrida (doble corrida, pool vacío, cuentas).
2. El guardrail de frescura de BIA (hard-fail a 30 días) — no se sabe si es un 4º check de esta
   etapa o vive en Date & Time Filters.
3. Dónde vive el override auditado (`force` + motivo) — esta etapa o Payment Filters.
4. Los 2 checks nuevos del transversal de la épica (pool no vacío, cuentas de las reglas existen)
   no tienen implementación real todavía — quedan como `NOT_IMPLEMENTED` en runtime, igual que
   hoy.
