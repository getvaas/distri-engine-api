package com.getvaas.distribution.engine.infrastructure.web.dto;

public record ListDistributionConfigsRequest(
        String name,
        Long masterTrustId,
        Long companyId,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {}
