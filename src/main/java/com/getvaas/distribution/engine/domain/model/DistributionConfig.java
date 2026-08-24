package com.getvaas.distribution.engine.domain.model;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;

import java.time.LocalDateTime;

public record DistributionConfig(
        String id,
        String name,
        Long companyId,
        Long masterTrustId,
        DistributionConfigStatus status,
        DistributionConfigPayload config,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
