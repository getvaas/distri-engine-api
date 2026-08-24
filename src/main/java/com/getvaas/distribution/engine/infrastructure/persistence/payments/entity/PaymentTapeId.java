package com.getvaas.distribution.engine.infrastructure.persistence.payments.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PK compuesta real de {@code payment_tape} — {@code (company_id, id)}. Usada como
 * {@code @IdClass} en {@link PaymentTapeEntity} para que cualquier acceso por id obligue a pasar
 * también {@code companyId} (la tabla está particionada por ese campo).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTapeId implements Serializable {
    private String id;
    private Long companyId;
}
