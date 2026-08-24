**Created at**: 2026-08-21
**Status**: Done
**Based on story**: @specs/20260821-155208_payment-tape-search-window-working-days-vpr-9662/story.md

# Plan: Ventana de búsqueda de payment tapes candidatos

### Goal
Calcular la ventana de fechas ajustada por días hábiles y traer, de la tabla real `payment_tape`, los
tapes candidatos (dentro de la ventana, no distribuidos todavía) para un borrower en una fecha dada.

### Context
- `PaymentsDataSourceConfig` — ya apunta a `payments_db`, con `hibernate.hbm2ddl.auto=none`. Esta
  historia agrega el primer mapeo **de solo lectura** sobre una tabla que no es nuestra.
- El `CREATE TABLE payment_tape` real (relevado en esta conversación) — se mapean solo las columnas que
  este ticket necesita (`id`, `company_id`, `payment_date`, `distribution_id`); el resto se agrega
  columna por columna a medida que otros tickets las necesiten (mismo criterio incremental que
  `DistributionConfigPayload`).
- **`payment_tape` está `PARTITION BY RANGE (company_id)`, con PK compuesta `(company_id, id)`** — toda
  query tiene que filtrar por `company_id` explícitamente para que MySQL pode particiones (partition
  pruning). Sin ese filtro, escanea las ~25 particiones completas — inaceptable en una tabla de este
  tamaño. Esto condiciona el diseño de la entidad y del repositorio (ver Public Contracts).
- `ResolveActiveDistributionConfigUseCase` (VPR-9660) — de ahí sale `daysBack` (Pool Strategy) y
  `country` (Deal Info); este ticket es el primer consumidor real de ambos juntos.
- **`borrowerId` = `companyId`** — mismo identificador, sin tabla de mapeo entre "borrower" (lenguaje de
  negocio) y "company" (columna real en `payment_tape` y en `DistributionConfig`). El `companyId` que ya
  usamos en toda la config (VPR-9644 en adelante) es directamente el filtro de partición contra
  `payment_tape.company_id` — no hay traducción de identidad que resolver.
- `docs/proceso-distribucion-unificado.md` Sección 1, paso 4a — `DefaultDistributablePaymentsFetcher` +
  `DateHelper.getWorkingDaysBack` es la referencia real de este cálculo en `master-trust-servicer-api`.

### Public Contracts
- **Domain**: `WorkingDaysCalculator.subtractWorkingDays(LocalDate date, int days, String country)` —
  hoy solo excluye fines de semana (mismo TODO de calendario de feriados que `BusinessDayCheck`,
  VPR-9661); `PaymentTapeCandidate(id, companyId, paymentDate)` — record mínimo, no expone todavía el
  resto de las columnas.
- **Persistence**: `PaymentTapeEntity` (read-only, `@Table(name = "payment_tape")`, solo las 4 columnas
  necesarias) con **PK compuesta real** vía `@IdClass(PaymentTapeId.class)` (`id` + `companyId`) — así
  `JpaRepository<PaymentTapeEntity, PaymentTapeId>` hace **imposible** escribir un `findById` que omita
  `companyId` por accidente. `PaymentTapeJPARepository.findCandidates(companyId, fromDate, untilDate)` —
  `companyId` siempre primer parámetro y obligatorio, filtra además `distribution_id IS NULL`. Ningún
  método del repositorio expone una query sin `companyId` en el WHERE.
- **Use case**: `FetchCandidatePaymentTapesUseCase.execute(companyId, date)`.
- **Endpoint**: `GET /distributions/candidates?companyId=&date=` (diagnóstico — devuelve el pool
  candidato sin ejecutar nada; sirve para probar este bloque antes de que exista el resto del pipeline).
- **Tests**: `WorkingDaysCalculatorTest`, `FetchCandidatePaymentTapesUseCaseTest`.

### Phases

#### Phase 1: Cálculo de ventana
[Aislado del resto — no depende de datos reales, solo de fechas.]
- [x] `WorkingDaysCalculator` (fin de semana; TODO feriados por país)
- [x] Tests: ventana que cruza un fin de semana, ventana de 0 días, país sin feriados considerados

#### Phase 2: Lectura de payment_tape
[El primer mapeo de solo lectura sobre una tabla que no es nuestra — partition-aware desde el diseño.]
- [x] `PaymentTapeId` (`@IdClass`: id + companyId) + `PaymentTapeEntity` (4 columnas)
- [x] `PaymentTapeJPARepository` con la query de candidatos (companyId siempre presente en el WHERE)
- [x] `PaymentTapeCandidate` (record de dominio)
- [x] Test/verificación: confirmar que la query generada incluye `company_id` en el WHERE (no solo que
  el resultado sea correcto — que el plan de ejecución use partition pruning)

#### Phase 3: Use case + endpoint + tests
[Conecta Pool Strategy + Deal Info (VPR-9660) con la lectura real.]
- [x] `FetchCandidatePaymentTapesUseCase` — resuelve config activa, calcula ventana, consulta candidatos
- [x] Endpoint diagnóstico `GET /distributions/candidates`
- [x] Tests: ventana correcta, excluye ya-distribuidos, excluye fuera de ventana, propaga el error de
  "sin config activa"

### Next Step
Implementado y con tests unitarios en verde. **Pendiente de verificación real**: el mapeo `@IdClass` +
`payment_tape` compila y los use cases pasan con Mockito, pero no se probó todavía contra una conexión
real a `payments_db` — no hay garantía de que Hibernate valide el metamodelo sin sorpresas hasta que
arranque contra la base real (es exactamente la prueba manual que el usuario va a evaluar hacer).
