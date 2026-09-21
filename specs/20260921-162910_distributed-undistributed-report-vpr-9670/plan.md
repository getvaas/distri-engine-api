**Created at**: 2026-09-21
**Status**: Draft
**Based on story**: @story.md

# Plan: Generar el reporte Excel de payment tapes distribuidos y no distribuidos

### Goal
Generar, on-demand y sin persistir nada, un `.xlsx` con 2 sheets ("Distributed Payments"/"Undistributed Payments") a partir de los payment tapes ya persistidos — replicando la forma real del reporte del motor Kotlin (`DistributedAndUndistributedReporter`), con las columnas que ya tenemos mapeadas hoy.

### Context
- `infrastructure/persistence/payments/entity/PaymentTapeEntity.java` — campos ya mapeados: `id, companyId, paymentDate, distributionId (String), paymentId, fundTransferId, netAmount, totalPayment, gatewayCode, ownerName`.
- `infrastructure/persistence/payments/PaymentTapeJPARepository.java` — hoy solo tiene `findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull`; necesita 2 métodos nuevos.
- `build.gradle` — agregar `org.apache.poi:poi-ooxml:5.4.0`.

### Public Contracts
- **Services**:
  - `GenerateDistributionReportUseCase.execute(Long companyId, Long distributionId): byte[]` — arma el `.xlsx` en memoria (`XSSFWorkbook`), lo serializa a `byte[]`, sin persistir nada. Convierte `distributionId` a `String` internamente para la query (columna real es `String`).
- **Persistence**:
  - `PaymentTapeJPARepository.findByCompanyIdAndDistributionId(Long companyId, String distributionId): List<PaymentTapeEntity>` (nuevo, sheet "Distributed Payments").
  - `PaymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(Long companyId): List<PaymentTapeEntity>` (nuevo, sheet "Undistributed Payments" — sin filtro de fecha, igual que el sistema real).
- **Estructura del Excel**: 2 sheets, header con las 10 columnas mapeadas (`Id, CompanyId, GatewayCode, PaymentDate, NetAmount, TotalPayment, OwnerName, DistributionId, PaymentId, FundTransferId`), una fila por tape.
- **Tests**: `GenerateDistributionReportUseCaseTest` (sheets correctos, columnas correctas, caso vacío sin error, verificación de que devuelve `.xlsx` válido parseable).

### Phases

#### Phase 1: Repository + use case + tests
- [x] Agregar `org.apache.poi:poi-ooxml:5.4.0` a `build.gradle`.
- [x] Agregar `findByCompanyIdAndDistributionId`/`findByCompanyIdAndDistributionIdIsNull` a `PaymentTapeJPARepository`.
- [x] Crear `GenerateDistributionReportUseCase` (`XSSFWorkbook`, 2 sheets, las 10 columnas mapeadas, devuelve `byte[]`).
- [x] Tests AAA (Mockito/AssertJ + Apache POI para parsear el resultado y verificar sheets/columnas/filas).

### Next Step
Única fase implementada. Pendiente: correr `./scripts/run-tests.sh` y confirmar que pasan antes de marcar Status Done.
