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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapeo de <b>escritura</b> sobre {@code assignment} (VPR-9669) — mismo dato que persiste
 * {@code CreateDistribution.apply()} en el motor real. Verificado contra la migración baseline
 * (`account_id`, `amount`, `currency`, `active`, `creation_date`, `last_update_date` NOT NULL) más
 * las columnas agregadas después (`V1.0.9`/`V1.0.10`): {@code concept} y {@code from_account_id}
 * son ambas opcionales — {@code fromAccountId} queda siempre {@code null} por ahora, ya que este
 * repo todavía no mueve plata entre cuentas (fuera de alcance, ver {@code CalculateAssignmentsUseCase}).
 */
@Entity
@Table(name = "assignment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @Column(name = "concept")
    private String concept;

    @Column(name = "from_account_id")
    private Long fromAccountId;
}
