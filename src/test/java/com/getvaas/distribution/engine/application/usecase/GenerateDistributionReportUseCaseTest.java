package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.infrastructure.persistence.payments.PaymentTapeJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateDistributionReportUseCaseTest {

    private static final Long COMPANY_ID = 3L;
    private static final Long DISTRIBUTION_ID = 427L;

    @Mock
    private PaymentTapeJPARepository paymentTapeJPARepository;
    @InjectMocks
    private GenerateDistributionReportUseCase useCase;

    private PaymentTapeEntity tape(String id, String distributionId, String ownerName) {
        return PaymentTapeEntity.builder()
                .id(id)
                .companyId(COMPANY_ID)
                .gatewayCode("GATEWAY_A")
                .paymentDate(LocalDateTime.of(2026, 9, 18, 10, 0))
                .netAmount(new BigDecimal("100.00"))
                .totalPayment(new BigDecimal("100.00"))
                .ownerName(ownerName)
                .distributionId(distributionId)
                .paymentId("payment-1")
                .fundTransferId("transfer-1")
                .build();
    }

    private XSSFWorkbook parse(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    void execute_hasExactlyTwoSheets_distributedAndUndistributed() throws IOException {
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionId(COMPANY_ID, "427")).thenReturn(List.of());
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(COMPANY_ID)).thenReturn(List.of());

        var bytes = useCase.execute(COMPANY_ID, DISTRIBUTION_ID);

        try (var workbook = parse(bytes)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Distributed Payments");
            assertThat(workbook.getSheetName(1)).isEqualTo("Undistributed Payments");
        }
    }

    @Test
    void execute_distributedSheet_containsHeaderAndOneRowPerTape() throws IOException {
        var tape1 = tape("pt-1", "427", "lender");
        var tape2 = tape("pt-2", "427", "lender");
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionId(COMPANY_ID, "427"))
                .thenReturn(List.of(tape1, tape2));
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(COMPANY_ID)).thenReturn(List.of());

        var bytes = useCase.execute(COMPANY_ID, DISTRIBUTION_ID);

        try (var workbook = parse(bytes)) {
            XSSFSheet sheet = workbook.getSheet("Distributed Payments");
            assertThat(sheet.getLastRowNum()).isEqualTo(2); // header (0) + 2 data rows (1,2)
            assertThat(cellValue(sheet.getRow(0), 0)).isEqualTo("Id");
            assertThat(cellValue(sheet.getRow(1), 0)).isEqualTo("pt-1");
            assertThat(cellValue(sheet.getRow(1), 6)).isEqualTo("lender");
            assertThat(cellValue(sheet.getRow(1), 7)).isEqualTo("427");
            assertThat(cellValue(sheet.getRow(2), 0)).isEqualTo("pt-2");
        }
    }

    @Test
    void execute_undistributedSheet_queriesWithoutDateFilter() throws IOException {
        var tape = tape("pt-3", null, null);
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionId(COMPANY_ID, "427")).thenReturn(List.of());
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(COMPANY_ID)).thenReturn(List.of(tape));

        var bytes = useCase.execute(COMPANY_ID, DISTRIBUTION_ID);

        try (var workbook = parse(bytes)) {
            XSSFSheet sheet = workbook.getSheet("Undistributed Payments");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(cellValue(sheet.getRow(1), 0)).isEqualTo("pt-3");
            assertThat(cellValue(sheet.getRow(1), 6)).isEmpty(); // ownerName null -> empty string
            assertThat(cellValue(sheet.getRow(1), 7)).isEmpty(); // distributionId null -> empty string
        }
    }

    @Test
    void execute_noTapesAtAll_producesEmptySheetsWithHeaderOnly() throws IOException {
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionId(COMPANY_ID, "427")).thenReturn(List.of());
        when(paymentTapeJPARepository.findByCompanyIdAndDistributionIdIsNull(COMPANY_ID)).thenReturn(List.of());

        var bytes = useCase.execute(COMPANY_ID, DISTRIBUTION_ID);

        try (var workbook = parse(bytes)) {
            assertThat(workbook.getSheet("Distributed Payments").getLastRowNum()).isEqualTo(0);
            assertThat(workbook.getSheet("Undistributed Payments").getLastRowNum()).isEqualTo(0);
        }
    }

    private String cellValue(Row row, int columnIndex) {
        return row.getCell(columnIndex).getStringCellValue();
    }
}
