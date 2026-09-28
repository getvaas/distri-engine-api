**Created at**: 2026-09-21
**Based on plan**: @plan.md
**Based on story**: @story.md

# Resume: Generar el reporte Excel de payment tapes distribuidos y no distribuidos

### Executive Summary
El motor ahora puede generar un reporte Excel (2 sheets: distribuidos / no distribuidos) para cualquier distribución ya persistida, replicando la forma real del reporte que usa `master-trust-servicer-api` en producción — cerrando el Bloque 5 (VPR-9670) del pipeline de ejecución.

### Technical Summary
- Investigando el ticket contra el código real (`DistributedAndUndistributedReporter.kt`) y un archivo real de PayJoy compartido por el usuario (108MB, ~676k filas combinadas) se corrigieron varias imprecisiones del ticket: son 2 sheets reales, no 3 (el tercer tab no existe en ningún lado del sistema real); "Alimentado por VPR-9640" era engañoso — VPR-9640 es sobre selección de adjuntos de notificación, no genera nada; el reporte real sí está conectado a la instrucción de transferencia (VPR-9639), agregándose a `distribution.documents`.
- Nuevo `GenerateDistributionReportUseCase`: recibe `companyId`+`distributionId`, consulta `payment_tape` (distribuidos: `distribution_id = X`; no distribuidos: `distribution_id IS NULL`, sin filtro de fecha, igual que el sistema real), arma un `.xlsx` con Apache POI (`XSSFWorkbook`), devuelve `byte[]`.
- Decisiones de alcance explícitas: sin tercer tab, sin persistir el archivo generado (se recalcula siempre on-demand — el sheet "no distribuidos" nunca es estable, cachearlo serviría snapshots viejos sin resolver el costo real), sin disparo automático desde `RunDistributionUseCase`, sin config-gate, `XSSFWorkbook` simple en vez del `SXSSFWorkbook` streaming real (documentado como límite conocido dado el volumen real confirmado de 650k+ filas), solo las 10 columnas ya mapeadas en `PaymentTapeEntity` (sin columnas dinámicas de `extraData`).
- **Sin endpoint HTTP todavía** — por pedido explícito del usuario: el use case queda listo para exponerse por un endpoint en una historia futura, sin wirearlo ahora.
- Nuevos métodos en `PaymentTapeJPARepository`: `findByCompanyIdAndDistributionId`, `findByCompanyIdAndDistributionIdIsNull`.
- Dependencia nueva: `org.apache.poi:poi-ooxml:5.4.0`.
- Tests: `GenerateDistributionReportUseCaseTest` (4 casos: estructura de 2 sheets, filas de distribuidos, no distribuidos sin filtro de fecha, caso vacío). Corridos vía `./scripts/run-tests.sh`, confirmados pasando por el usuario.

### Phases Completed
- [x] **Phase 1: Repository + use case + tests** — `GenerateDistributionReportUseCase`, 2 métodos nuevos de repositorio, dependencia Apache POI, y su suite de tests.
