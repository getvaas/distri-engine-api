package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Reporte distribuido/no-distribuido (Bloque 5, paso 10d, VPR-9670) — replica la forma real del
 * motor Kotlin (`DistributedAndUndistributedReporter`), verificada contra un archivo real de
 * PayJoy: 2 sheets exactos ("Distributed Payments"/"Undistributed Payments"), sin un tercer tab
 * ("payments sin tape" no existe en el sistema real). Las columnas son solo las que ya están
 * mapeadas en {@link PaymentTapeEntity} — el sistema real tiene más (incluidas columnas dinámicas
 * por borrower vía {@code extraData}), fuera de alcance acá.
 * <p>
 * Se recalcula siempre on-demand, sin persistir el archivo generado — decisión explícita: el sheet
 * "Undistributed Payments" nunca es estable (cambia todos los días), así que cachear el archivo
 * completo serviría snapshots desactualizados sin resolver el costo real. Usa {@link XSSFWorkbook}
 * simple, no el streaming ({@code SXSSFWorkbook}) del sistema real — límite conocido: no escala al
 * volumen real confirmado (650k+ filas en un solo borrower real), aceptable porque
 * distri-engine-api no tiene volumen real todavía. Sin disparo automático ni config-gate — es
 * 100% on-demand, pensado para exponerse por un endpoint en una historia futura.
 */
@Component
@RequiredArgsConstructor
public class GenerateDistributionReportUseCase {

    private static final String DISTRIBUTED_SHEET_NAME = "Distributed Payments";
    private static final String UNDISTRIBUTED_SHEET_NAME = "Undistributed Payments";
    private static final List<String> HEADERS = List.of(
            "Id", "CompanyId", "GatewayCode", "PaymentDate", "NetAmount", "TotalPayment",
            "OwnerName", "DistributionId", "PaymentId", "FundTransferId");

    private final PaymentTapeJPARepository paymentTapeJPARepository;

    public byte[] execute(Long companyId, Long distributionId) {
        var distributed = paymentTapeJPARepository.findByCompanyIdAndDistributionId(
                companyId, String.valueOf(distributionId));
        var undistributed = paymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(companyId);

        try (var workbook = new XSSFWorkbook()) {
            writeSheet(workbook, DISTRIBUTED_SHEET_NAME, distributed);
            writeSheet(workbook, UNDISTRIBUTED_SHEET_NAME, undistributed);

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el reporte distribuido/no-distribuido", e);
        }
    }

    private void writeSheet(XSSFWorkbook workbook, String sheetName, List<PaymentTapeEntity> tapes) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        writeHeaderRow(sheet);
        for (int i = 0; i < tapes.size(); i++) {
            writeDataRow(sheet, i + 1, tapes.get(i));
        }
    }

    private void writeHeaderRow(XSSFSheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
            header.createCell(i).setCellValue(HEADERS.get(i));
        }
    }

    private void writeDataRow(XSSFSheet sheet, int rowIndex, PaymentTapeEntity tape) {
        Row row = sheet.createRow(rowIndex);
        setCell(row, 0, tape.getId());
        setCell(row, 1, tape.getCompanyId() != null ? String.valueOf(tape.getCompanyId()) : null);
        setCell(row, 2, tape.getGatewayCode());
        setCell(row, 3, tape.getPaymentDate() != null ? tape.getPaymentDate().toString() : null);
        setCell(row, 4, tape.getNetAmount() != null ? tape.getNetAmount().toPlainString() : null);
        setCell(row, 5, tape.getTotalPayment() != null ? tape.getTotalPayment().toPlainString() : null);
        setCell(row, 6, tape.getOwnerName());
        setCell(row, 7, tape.getDistributionId());
        setCell(row, 8, tape.getPaymentId());
        setCell(row, 9, tape.getFundTransferId());
    }

    private void setCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex).setCellValue(value != null ? value : "");
    }
}
