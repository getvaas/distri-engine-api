**Created at**: 2026-09-21
**Status**: In Progress
**Original input**: @original_request.md
**Plan implemented**: —

# Story: Generar el reporte Excel de payment tapes distribuidos y no distribuidos

### Description
Hoy, una vez que una distribución se persiste (VPR-9669), no hay ninguna forma de ver un detalle exportable de qué payment tapes quedaron distribuidos en esa corrida ni cuáles siguen pendientes — solo se puede consultar directo contra la base. Esta historia agrega la lógica que genera ese reporte en formato Excel, con 2 sheets ("Distributed Payments"/"Undistributed Payments"), replicando el reporte real que ya existe en el motor Kotlin (`DistributedAndUndistributedReporter`), recalculado on-demand a partir de los datos ya persistidos — sin necesidad de guardar el archivo generado en ningún lado.

### Acceptance Criteria
- [ ] **Given** un `companyId` y un `distributionId` reales, **When** se genera el reporte, **Then** el sheet "Distributed Payments" contiene una fila por cada payment tape con ese `distributionId`, con las columnas ya mapeadas (`id, companyId, gatewayCode, paymentDate, netAmount, totalPayment, ownerName, distributionId, paymentId, fundTransferId`).
- [ ] **Given** el mismo `companyId`, **When** se genera el reporte, **Then** el sheet "Undistributed Payments" contiene todas las payment tapes de esa company con `distribution_id IS NULL`, sin filtro de fecha, con las mismas columnas.
- [ ] **Given** ninguna tape distribuida para ese `distributionId` (caso raro, pero posible), **When** se genera el reporte, **Then** el sheet "Distributed Payments" queda vacío (solo el header), sin error.
- [ ] **Given** el archivo generado, **When** se abre, **Then** es un `.xlsx` válido con exactamente esos 2 sheets, sin un tercer tab.

### Additional Context
Fuera de alcance explícito de esta historia (decisiones tomadas tras investigar el sistema real, documentadas en `original_request.md`): tercer tab ("payments sin tape", no existe en el sistema real), persistencia/cache del archivo generado, disparo automático desde `RunDistributionUseCase`, config-gate (flag global/por-borrower), `SXSSFWorkbook` streaming, columnas dinámicas de `extraData` o cualquier campo no mapeado hoy en `PaymentTapeEntity`, y el endpoint HTTP que lo expondría (se construye el use case listo para ser expuesto más adelante, no el endpoint en sí).
