**Created at**: 2026-08-24
**Status**: Approved
**Based on story**: @specs/20260824-134720_readiness-checks-per-check-failure-behavior-vpr-9637-9638/story.md

# Plan: Readiness Checks — comportamiento de falla por check

### Goal
Reemplazar el `failureAction`/`retry` global de `ReadinessChecksConfig` por un setting
independiente por cada check habilitado, y corregir la atribución de tickets (VPR-9637/9638, no
VPR-9661) en la documentación del proyecto.

### Context
- `ReadinessChecksConfig.java` — hoy `(enabledChecks: List<ReadinessCheckType>, failureAction,
  retry)`; pasa a `(checks: List<ReadinessCheckSetting>)`.
- `UpdateReadinessChecksConfigUseCase.java` y `UpdateReadinessChecksConfigRequest.java` — se
  reescriben para el nuevo shape.
- `ReadinessCheckRunner.run(...)` sigue operando sobre `List<ReadinessCheckType>` — no cambia su
  firma; `RunReadinessChecksUseCase` extrae los `type()` de la nueva lista antes de llamarlo. El
  motor de ejecución (VPR-9661) no se toca más allá de ese ajuste de una línea.
- `failureAction`/`retry` quedan como metadata de config — aplicarlos de verdad durante una
  corrida real (bloquear vs. particionar vs. solo reportar) es trabajo de la etapa de ejecución
  completa, fuera de esta historia.

### Public Contracts
- **Domain**: `ReadinessCheckSetting(type, failureAction, retry)` (nuevo);
  `ReadinessChecksConfig(checks: List<ReadinessCheckSetting>)` (reemplaza el shape anterior).
- **DTO**: `ReadinessCheckSettingRequest(type, failureAction, retry)` (nuevo);
  `UpdateReadinessChecksConfigRequest(checks: List<ReadinessCheckSettingRequest>)` (reemplaza el
  shape anterior).
- **Endpoint**: sin cambios — `PUT /configs/{id}/readiness-checks` (ya existe).
- **Tests**: reescribir `UpdateReadinessChecksConfigUseCaseTest`; ajustar
  `RunReadinessChecksUseCaseTest` al nuevo shape de `ReadinessChecksConfig` (extracción de types).

### Phases

#### Phase 1: Modelo tipado por-check
[Reemplaza enabledChecks+failureAction+retry global por una lista de settings.]
- [ ] `ReadinessCheckSetting` (domain)
- [ ] `ReadinessChecksConfig` reescrito a `checks: List<ReadinessCheckSetting>`
- [ ] `ReadinessCheckSettingRequest` (DTO) + `UpdateReadinessChecksConfigRequest` reescrito

#### Phase 2: Use case + validaciones
- [ ] Lista no enviada/vacía → default: los 3 checks conocidos con `PAUSE_AND_ALERT`/`NEXT_CYCLE`
- [ ] `type` requerido por setting
- [ ] Sin `type` repetido en la lista
- [ ] `failureAction`/`retry` default por-setting si no se especifican (no afecta a otros checks)

#### Phase 3: Ajuste del motor de ejecución (mínimo, VPR-9661 no se re-abre)
- [ ] `RunReadinessChecksUseCase` extrae `List<ReadinessCheckType>` desde
  `readinessChecksConfig.checks()` antes de llamar a `ReadinessCheckRunner.run(...)` (sin cambiar
  la firma del runner)

#### Phase 4: Tests
- [ ] Checks con `failureAction`/`retry` propios → persisten independientes entre sí
- [ ] Check sin `failureAction`/`retry` → usa defaults solo para ese check
- [ ] `type` repetido → error
- [ ] Lista vacía/no enviada → default a los 3 checks conocidos
- [ ] `RunReadinessChecksUseCaseTest` sigue pasando con el nuevo shape (ajustar construcción del
  fixture, no la lógica del test)

### Next Step
Corregir la atribución en `docs/architecture/distribution-config-schema.md` (referenciaba
VPR-9661 para la config, debe decir VPR-9637/9638) y en cualquier `resume.md` previo que haga la
misma asociación incorrecta.
