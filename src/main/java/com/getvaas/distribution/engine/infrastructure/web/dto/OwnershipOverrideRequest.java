package com.getvaas.distribution.engine.infrastructure.web.dto;

public record OwnershipOverrideRequest(
        String key,
        String owner
) {}
