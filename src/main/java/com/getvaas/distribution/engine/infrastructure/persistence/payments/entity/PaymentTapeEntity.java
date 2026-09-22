package com.getvaas.distribution.engine.infrastructure.persistence.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapeo de <b>solo lectura</b> sobre {@code payment_tape} — la escribe {@code payment-data-extractor},
 * no este servicio. Mapea solo las columnas que los tickets ya construidos necesitan (VPR-9662); se
 * agregan más a medida que otros tickets las requieran (Payment Filters, ownership, etc.), nunca de
 * más.
 * <p>
 * La tabla está {@code PARTITION BY RANGE (company_id)} — por eso {@code @IdClass}: obliga a pasar
 * {@code companyId} en cualquier acceso por id, para no poder generar (ni pedir por accidente) una
 * query sin el filtro de partición.
 * <p>
 * {@code total_payment} (no {@code gross_amount} — esa columna nunca existió en la tabla real,
 * verificado contra el schema real de {@code payments_db}) es la que responde a la opción
 * {@code gross_amount} de {@code amountField}/Payment Filters — remapeo pragmático mientras no haya
 * un reemplazo mejor confirmado (el candidato {@code net_amount + fee_amount} no sirve hoy porque
 * {@code fee_amount} siempre viene null, bug conocido del extractor, VPR-9666).
 */
@Entity
@Table(name = "payment_tape")
@IdClass(PaymentTapeId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTapeEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Id
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "distribution_id")
    private String distributionId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "fund_transfer_id")
    private String fundTransferId;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Column(name = "total_payment")
    private BigDecimal totalPayment;

    @Column(name = "gateway_code")
    private String gatewayCode;

    @Column(name = "owner_name")
    private String ownerName;
}
