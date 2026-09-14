**Created at**: 2026-09-14
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @plan.md

# Story: Resolver el owner de cada payment tape (PAYMENT_TAPE_FIELD)

### Description
El pool de fondos elegibles (Bloque 2, VPR-9662/9663/9664) llega sin ningún owner resuelto —
nada asigna cada payment tape a la company que la va a recibir. Esto cierra el primer tramo del
Bloque 3 del pipeline real: resolver el owner declarado directamente en el payment tape, con el
fallback correcto (nunca `null`, corrigiendo el bug real documentado en el motor actual donde un
owner nulo tira NPE y mata toda la corrida).

### Acceptance Criteria
- [x] **Given** una config con `ownership.source.sourceType=PAYMENT_TAPE_FIELD` y `field="owner_name"`, **When** se resuelve el pool, **Then** cada `PoolFund` trae el owner leído de la columna `owner_name` del payment tape.
- [x] **Given** un payment tape con `owner_name` nulo o vacío y `defaultOwner` configurado, **When** se resuelve, **Then** el owner cae al `defaultOwner`.
- [x] **Given** un payment tape con `owner_name` nulo y sin `defaultOwner` configurado, **When** se resuelve, **Then** el owner es `"UNDEFINED"` — nunca `null`, nunca una excepción.
- [x] **Given** una config sin `ownership` configurado en absoluto, **When** se resuelve el pool, **Then** el owner de cada fund es `"UNDEFINED"`.
- [x] **Given** `field` distinto de `"owner_name"`, `sourceType=OWNERSHIP_API`, o `crossValidation.enabled=true`, **When** se intenta resolver, **Then** falla explícito en vez de simular un resolver que no existe.

### Additional Context
- Jira: VPR-9665. Referencia: `docs/proceso-distribucion-unificado.md` Sección 1 paso 5a.
- **Fuera de alcance, decisión explícita del usuario tras investigar el código real**: `OwnershipSourceType.OWNERSHIP_API` (requiere `OwnerAtomHttpClient` + `CompanyClient`, ninguno existe en este repo) y el cross-check real de VPR-9636 (las 4 estrategias de mismatch) — quedan para un ticket futuro dedicado al Atom tracker.
- Los 3 resolvers adicionales de VPR-9635 (Somos, Rapicredit, Finamco) siguen fuera de alcance, consistente con esa historia.
- `"UNDEFINED"` como marcador de owner sin resolver es intencional — corrige el bug real documentado (`p.ownerName!!` en el motor Kotlin actual tira NPE en vez de particionar como ownerless). La partición ownerless vs. distribuibles en sí (paso 6 del pipeline) queda para un ticket futuro.
