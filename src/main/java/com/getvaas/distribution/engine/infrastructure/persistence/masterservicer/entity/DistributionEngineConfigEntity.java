package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_engine_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionEngineConfigEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "master_trust_id")
    private Long masterTrustId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DistributionConfigStatus status;

    @Column(name = "config_json", nullable = false, columnDefinition = "LONGTEXT")
    private String configJson;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
