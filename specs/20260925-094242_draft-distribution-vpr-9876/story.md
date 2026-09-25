**Created at**: 2026-09-25
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: —

# Story: Ejecutar una distribución en modo borrador, revisable antes de confirmarse

### Description
Hoy toda distribución que corre queda inmediatamente final: se persiste, se marcan los payment tapes como distribuidos, y se dispara la notificación — sin ningún paso intermedio de revisión. En el sistema real, algunos deals (hoy solo ADDI, mediante un mecanismo real llamado "cash release") necesitan que la distribución quede primero en un estado "borrador" — ya calculada y con los fondos reservados, pero sin notificar ni confirmarse — para poder revisarla/conciliarla antes de darla por buena. Esta historia agrega ese modo borrador como una config explícita por deal (a diferencia del sistema real, que lo resuelve hoy con un borrower-code hardcodeado en infraestructura, no en la config de negocio), junto con un paso manual de aprobación que confirma la distribución existente y recién ahí dispara la notificación.

### Acceptance Criteria
- [ ] **Given** una config con el modo borrador habilitado, **When** se corre la distribución, **Then** se persiste con estado `DRAFT`, los payment tapes quedan marcados/reservados contra esa distribución, pero no se dispara ninguna notificación.
- [ ] **Given** una config sin el modo borrador habilitado (comportamiento actual, default), **When** se corre, **Then** el comportamiento es idéntico al de hoy — sin ningún cambio para los deals existentes.
- [ ] **Given** una distribución en estado `DRAFT`, **When** se aprueba manualmente, **Then** su estado pasa a `APPROVED` (misma distribución, mismo id — no se crea una nueva), y recién en ese momento se dispara la notificación del resultado.
- [ ] **Given** un intento de aprobar una distribución que no está en estado `DRAFT` (ya aprobada, o inexistente), **When** se llama al endpoint de aprobación, **Then** falla explícito, sin cambiar nada.

### Additional Context
- Investigación verificada contra `master-trust-servicer-api` (rama `feat/draft-distribution-as-distribution`, dueño del brief): el mecanismo de fondo (`Distribution` con `status=draft`, aprobar en el mismo registro agregando assignments finales) es genérico en el código, pero **hoy solo lo usa ADDI en producción** — el gating real no está en ninguna config de negocio, está hardcodeado en 2 lugares: un cron de Terraform (`eventbridge-execute-addi-start-cash-release`, `borrower_code: "ADDI"`) y un `if (borrower == Borrower.ADDI)` en un endpoint de sync/dev. No hay ningún flag de "modo borrador" en la config real de distribución.
- El flujo real completo se llama "cash release": lo que arranca el draft (`StartCashReleaseFromMaster`) es solo la mitad de esta historia; la otra mitad real (`FinishCashReleaseFromMaster`) recalcula los montos finales a partir de `reconciliationResults`, un dato que llega por una cola SQS separada desde un servicio externo de reconciliación que no está visible desde ningún repo — **eso queda explícitamente fuera de esta historia**, es un ticket aparte que va a necesitar que alguien confirme el contrato real de ese dato externo. Acá, "aprobar" es una transición de estado simple sobre los mismos assignments ya calculados, no un recálculo contra reconciliación.
- De paso, esta historia corrige un desalineamiento real encontrado: `PersistDistributionUseCase` (VPR-9669) persiste `status` como `"CALCULATED"`/`"NOTHING_DISTRIBUTABLE"` (inventados en su momento) en vez de los valores reales confirmados ahora (`approved`/`nothing_distributable`, y ahora se suma `draft`) — como escribimos directo en el mismo schema/tabla real (`master_trust_servicer.distribution`), esto importa para cualquier lectura futura desde herramientas del sistema real.
