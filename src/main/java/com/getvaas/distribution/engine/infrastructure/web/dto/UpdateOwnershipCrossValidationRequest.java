package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.OwnershipMismatchStrategy;

public record UpdateOwnershipCrossValidationRequest(
        Boolean enabled,
        OwnershipMismatchStrategy mismatchStrategy
) {}
