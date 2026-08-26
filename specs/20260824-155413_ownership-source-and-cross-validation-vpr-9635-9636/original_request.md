# Original Request

**Sources**:
- Jira ticket [VPR-9635](https://pmvaas1.atlassian.net/browse/VPR-9635) — "Ownership: Source (seguimos igual)"
- Jira ticket [VPR-9636](https://pmvaas1.atlassian.net/browse/VPR-9636) — "Ownership: cross validation"

**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## VPR-9635 — Source (extracto)
2 fuentes excluyentes: Ownership API (por contract_id) o Payment Tape Field. Resuelto: Finamco y
Liquitech son casos legítimos de "Payment Tape Field" (no una 3ra fuente). Gap detectado: el
selector de campo del wizard no soporta sub-paths de JSON — no aplica a nuestro backend porque el
campo ya es `String` libre. Fuera de alcance: capa de normalización/alias (Finamco/Liquitech).
Pendiente: si "owner por defecto" es un caso real.

## VPR-9636 — Cross validation (extracto)
Cross-check tape vs. Ownership API, 4 estrategias de mismatch (E1): `API_WINS`/`TAPE_WINS`/
`BLOCK_PAYMENT`/`BLOCK_DISTRIBUTION`. Finkargo Colombia ya implementa `API_WINS` en producción
(Python, `task.py:531, 819-824`) — referencia de aceptación, no diseño desde cero. Abiertas:
confirmar alcance de las 4 estrategias vs. solo `API_WINS`; fallback si la API cae; reuso de la
partición ownerless para `BLOCK_PAYMENT`; NPE de `p.ownerName!!` (bug de código, no bloquea).

## User additions (this session)

- El usuario indicó que el negocio ya estaba claro y pidió avisar solo si había dudas reales.
- Al preguntar sobre las 4 estrategias de mismatch, el usuario expresó que la pregunta era
  innecesaria dado que la épica ya lo define — se procedió con las 4 completas.
- Al preguntar sobre el fallback si la API cae, el usuario compartió el código real de
  `OwnerNameResolver.kt` (`master-trust-servicer-api`) explicando cómo funciona hoy la resolución
  de owner por contract_id: cae a una cadena de fallback (`legacy-getter → defaultOwnerCompanyId →
  borrower → UNDEFINED`) cuando el contrato no está mapeado (no fatal, partición silenciosa como
  ownerless); pero si el contrato SÍ está mapeado y resuelve a una compañía inexistente, lanza una
  excepción no capturada que tumba toda la corrida del borrower ese día
  (`DistributorCalculator.kt:150-157` → `DistributionRunner.kt:147-170`). El usuario aclaró que
  esta respuesta se basaba en el funcionamiento de otra API/sistema real (el motor de producción
  actual), como referencia de cómo deberíamos pensar el comportamiento — no necesariamente
  como una respuesta directa a "qué pasa si la Ownership API externa está caída", que sigue sin
  resolverse y queda fuera de esta historia.
- Basado en esto, `defaultOwner` se confirma como concepto real (no especulativo) y se modela como
  campo opcional.
