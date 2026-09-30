package com.getvaas.distribution.engine.infrastructure.web.dto;

public record ListDistributionsRequest(
        Long masterTrustServicerId,
        String status,
        int page,
        int size
) {}
