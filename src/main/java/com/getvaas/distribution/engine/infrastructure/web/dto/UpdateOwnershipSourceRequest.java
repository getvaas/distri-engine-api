package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;

public record UpdateOwnershipSourceRequest(
        OwnershipSourceType sourceType,
        String field,
        String defaultOwner
) {}
