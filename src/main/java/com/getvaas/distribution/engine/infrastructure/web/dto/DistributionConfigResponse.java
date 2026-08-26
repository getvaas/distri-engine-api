package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.PaymentFiltersConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
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
        PaymentFiltersConfig paymentFilters,
        DistributionRulesConfig rules,
        OwnershipConfig ownership,
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
                domain.config() != null ? domain.config().paymentFilters() : null,
                domain.config() != null ? domain.config().rules() : null,
                domain.config() != null ? domain.config().ownership() : null,
                domain.config() != null ? domain.config().readinessChecks() : null,
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
