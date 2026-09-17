package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapeo sobre {@code distribution} — de solo lectura para el check de duplicados (VPR-9661,
 * {@code NoDuplicateDistributionCheck}), y de <b>escritura</b> para persistir una distribución
 * calculada (VPR-9669, {@code PersistDistributionUseCase}), mismo patrón que
 * {@code CreateDistribution.apply()} en el motor real. {@code firstPaymentDate}/{@code lastPaymentDate}
 * son {@code NOT NULL} en la tabla real — nunca dejarlas sin resolver al guardar. La relación hacia
 * {@code assignment} usa la tabla real {@code distribution_assignments}, donde cada assignment
 * pertenece a una única distribución (columna {@code assignments_id} tiene constraint UNIQUE) — por
 * eso es {@code @OneToMany}, no un many-to-many real pese a la tabla intermedia.
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

    @Column(name = "first_payment_date")
    private LocalDateTime firstPaymentDate;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "distribution_assignments",
            joinColumns = @JoinColumn(name = "distribution_entity_id"),
            inverseJoinColumns = @JoinColumn(name = "assignments_id")
    )
    @Builder.Default
    private List<AssignmentEntity> assignments = List.of();
}
