**Created at**: 2026-09-17
**Status**: Done
**Original input**: @original_request.md
**Plan implemented**: @specs/20260917-120745_persist-distribution-and-mark-payment-tapes-vpr-9669/plan.md

# Story: Persistir la distribución y marcar payment tapes como distribuidos

### Description
Hoy el motor de ejecución calcula el pool elegible, lo particiona por ownership y arma los assignments por regla (VPR-9662 a 9668), pero nunca lo deja grabado — es pura simulación en memoria. Esta historia cierra el Bloque 5: persiste la distribución calculada (`Distribution`+`Assignment`) en las tablas reales de `master-trust-servicer-api`, del mismo modo que `CreateDistribution.apply()` lo hace hoy en el motor Kotlin, y marca cada payment tape distribuido con su `distributionId` para que no vuelva a ser candidato en una corrida futura.

### Acceptance Criteria
- [ ] **Given** una distribución con al menos un `Assignment` calculado, **When** se persiste, **Then** se graba una fila en `distribution` (`status=CALCULATED`) y una fila en `assignment` por cada `Assignment`, vinculadas vía `distribution_assignments`, todo en una única transacción.
- [ ] **Given** una distribución sin ningún `Assignment` (pool vacío), **When** se persiste, **Then** se graba `distribution` con `status=NOTHING_DISTRIBUTABLE` y ninguna fila en `assignment`.
- [ ] **Given** una distribución persistida exitosamente, **When** se marcan los payment tapes, **Then** cada fila de `payment_tape` correspondiente a un fondo de `PartitionedPoolFunds.distributable()` queda con su `distribution_id` actualizado.
- [ ] **Given** fondos en `PartitionedPoolFunds.ownerless()`, **When** se marcan los payment tapes, **Then** esos fondos NO se marcan como distribuidos (siguen siendo candidatos en la próxima corrida).
- [ ] **Given** la distribución calculada, **When** se persiste, **Then** `distribution.first_payment_date`/`last_payment_date` reflejan el mínimo/máximo `paymentDate` de los fondos distribuibles, y `assignment.currency`/`concept` se completan según lo definido (currency del deal; concept de la descripción de la regla o el owner).
- [ ] **Given** un error al persistir `distribution`/`assignment`, **When** ocurre, **Then** no queda ninguna fila parcial grabada en esa transacción. **Given** un error al marcar `payment_tape` después de persistir la distribución exitosamente, **When** ocurre, **Then** la distribución ya persistida no se revierte (son 2 datasources distintos, sin transacción distribuida) — falla explícito igual, pero sin comprometer lo ya grabado.

### Additional Context
Verificado contra `master-trust-servicer-api`: `CreateDistribution.apply()` (persistencia de `Distribution`/`Assignment`) y el marcado de `payment_tape` son 2 mecanismos separados en el motor real (el segundo solo se alcanza vía el pipeline interno completo, no vía la creación pública de una distribución) — acá se implementan ambos, dentro de esta misma historia, por decisión explícita del usuario. Se escribe directo a las tablas de `master_trust_servicer` (mismo datasource ya conectado, mismo patrón interno que el motor real) en vez de llamar al endpoint HTTP público que usan integradores externos (scrapy-lambdas/JTP/Rapicredit/Solvento). No incluye el reporte distribuido/no-distribuido (VPR-9670) ni ningún endpoint/scheduler nuevo para disparar la corrida (gap ya conocido, fuera de alcance).
