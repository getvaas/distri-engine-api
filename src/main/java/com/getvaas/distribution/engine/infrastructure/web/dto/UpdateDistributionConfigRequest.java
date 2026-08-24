package com.getvaas.distribution.engine.infrastructure.web.dto;

public record UpdateDistributionConfigRequest(
        String name,
        Long masterTrustId,
        String country,
        String currency
) {}
