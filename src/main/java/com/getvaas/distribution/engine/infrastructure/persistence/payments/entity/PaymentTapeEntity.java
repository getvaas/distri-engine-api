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

import java.time.LocalDateTime;

/**
 * Mapeo de <b>solo lectura</b> sobre {@code payment_tape} — la escribe {@code payment-data-extractor},
 * no este servicio. Mapea solo las 4 columnas que este ticket necesita (VPR-9662); se agregan más a
 * medida que otros tickets las requieran (Payment Filters, cálculo de monto, ownership, etc.), nunca de
 * más.
 * <p>
 * La tabla está {@code PARTITION BY RANGE (company_id)} — por eso {@code @IdClass}: obliga a pasar
 * {@code companyId} en cualquier acceso por id, para no poder generar (ni pedir por accidente) una
 * query sin el filtro de partición.
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
}
