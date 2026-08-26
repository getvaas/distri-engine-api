**Created at**: 2026-08-24
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9635 @https://pmvaas1.atlassian.net/browse/VPR-9636
**Plan implemented**: —

# Story: Ownership — Source y Cross Validation

### Description
Cada deal necesita definir de dónde sale el owner de un pago (una API externa de ownership, o una
columna del propio payment tape) y, cuando la fuente es el tape, si esa declaración se cruza
contra la API como control independiente — y qué hacer si no coinciden.

### Acceptance Criteria
- [ ] **Given** `sourceType=OWNERSHIP_API` con un `field` (columna de contract_id), **When** se
  guarda, **Then** la config persiste el tipo de fuente y el campo tal cual.
- [ ] **Given** `sourceType=PAYMENT_TAPE_FIELD` con un `field` (columna del owner, soporta
  sub-path tipo `extra_data.aux_var_3` porque es un `String` libre), **When** se guarda, **Then**
  la config persiste igual.
- [ ] **Given** cualquier `sourceType` sin `field`, **When** se intenta guardar, **Then** se
  rechaza — ambas fuentes necesitan saber qué columna leer.
- [ ] **Given** `defaultOwner` sin especificar, **When** se guarda, **Then** queda `null` — es
  opcional, no todos los deals lo necesitan.
- [ ] **Given** una config de cross-validation con `enabled=true` y una `mismatchStrategy` de las
  4 (`API_WINS`/`TAPE_WINS`/`BLOCK_PAYMENT`/`BLOCK_DISTRIBUTION`), **When** se guarda, **Then**
  persiste tal cual.
- [ ] **Given** `enabled=false`, **When** se guarda, **Then** `mismatchStrategy` se ignora y se
  persiste `null` — no aplica ninguna estrategia si el cross-check está apagado.

### Additional Context
Las 2 fuentes (`OWNERSHIP_API`, `PAYMENT_TAPE_FIELD`) son excluyentes, mockup confirma selección
única. `field` es un `String` libre (mismo patrón que `amountField` en Pool Strategy) — su
significado cambia según `sourceType`: columna de `contract_id` para consultar la API, o columna
del owner declarado en el tape (soporta rutas dentro de JSON como `extra_data.aux_var_3` sin
cambios de código, porque no hay validación contra columnas reales).

Verificado contra el código real (`OwnerNameResolver.kt`, `master-trust-servicer-api`):
`defaultOwner` es un concepto real y load-bearing hoy — el resolver cae a una cadena de fallback
(`legacy-getter → defaultOwnerCompanyId → borrower → UNDEFINED`) cuando un contrato no está
mapeado, y esos pagos quedan particionados como "ownerless" (silencioso, no fatal). **Riesgo real
detectado, documentado, no resuelto aquí**: si el contrato SÍ está mapeado pero resuelve a una
compañía inexistente, el resolver lanza una excepción no capturada que tumba toda la corrida del
borrower ese día (`DistributorCalculator.kt:150-157` → `DistributionRunner.kt:147-170`) — es un
bug de código del motor real, no un gap de configuración; no bloquea esta historia pero hay que
tenerlo presente antes de activar cross-validation en producción para cualquier deal.

Las 4 estrategias de mismatch completas (no solo `API_WINS`, el único patrón validado hoy en
producción vía Finkargo) están en el alcance, porque la épica (E1) las define así explícitamente.
Explícitamente fuera de esta historia, documentado como riesgo abierto: el fallback si la
Ownership API externa no responde (timeout/reintentos) — no hay un caso real todavía que dicte
cómo debería comportarse por deal; que `BLOCK_PAYMENT` reuse la partición de ownerless ya existente
(evento `DISTRIBUTION_UNKNOWN_OWNER_PAYMENTS`) es una decisión de la etapa de ejecución, no de esta
historia. La capa de normalización/transformación de owner (alias de Finamco, fallback de
Liquitech) sigue fuera de alcance, ya documentada en VPR-9635.
