package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;

import java.util.List;

public record UpdateOwnershipSourceRequest(
        OwnershipSourceType sourceType,
        String field,
        String defaultOwner,
        List<OwnershipOverrideRequest> overrides
) {}
