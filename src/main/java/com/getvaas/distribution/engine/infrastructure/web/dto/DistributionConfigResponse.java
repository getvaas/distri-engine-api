package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.DistributablePaymentsConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;

import java.time.LocalDateTime;

public record DistributionConfigResponse(
        String id,
        String name,
        Long companyId,
        Long masterTrustId,
        DistributionConfigStatus status,
        String country,
        String currency,
        PoolConfig pool,
        DistributablePaymentsConfig distributablePayments,
        DistributionRulesConfig rules,
        ReadinessChecksConfig readinessChecks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DistributionConfigResponse from(DistributionConfig domain) {
        return new DistributionConfigResponse(
                domain.id(),
                domain.name(),
                domain.companyId(),
                domain.masterTrustId(),
                domain.status(),
                domain.config() != null ? domain.config().country() : null,
                domain.config() != null ? domain.config().currency() : null,
                domain.config() != null ? domain.config().pool() : null,
                domain.config() != null ? domain.config().distributablePayments() : null,
                domain.config() != null ? domain.config().rules() : null,
                domain.config() != null ? domain.config().readinessChecks() : null,
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
