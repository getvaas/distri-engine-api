package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapeo de <b>solo lectura</b> sobre {@code account_balance} — la escribe/administra
 * {@code master-trust-servicer-api}, no este servicio (VPR-9668, chequeo de balance de
 * Distribution Rules). Mapea solo las columnas que el chequeo necesita; nunca migrar ni alterar
 * esta tabla desde acá. Verificado contra {@code V1.0.11__create_account_balance.sql}.
 */
@Entity
@Table(name = "account_balance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "current_balance")
    private BigDecimal currentBalance;

    @Column(name = "projected_balance")
    private BigDecimal projectedBalance;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;
}
