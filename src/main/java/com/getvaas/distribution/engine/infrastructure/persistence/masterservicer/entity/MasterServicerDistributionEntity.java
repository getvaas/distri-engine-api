package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mapeo de <b>solo lectura</b> sobre {@code distribution} — la escribe/administra
 * {@code master-trust-servicer-api}, no este servicio (VPR-9661, NoDuplicateDistributionCheck).
 * Mapea solo las columnas que este check necesita; nunca migrar ni alterar esta tabla desde acá.
 */
@Entity
@Table(name = "distribution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterServicerDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_trust_servicer_id")
    private Long masterTrustServicerId;

    @Column(name = "status")
    private String status;

    @Column(name = "distribution_date")
    private LocalDateTime distributionDate;

    @Column(name = "active")
    private Boolean active;
}
