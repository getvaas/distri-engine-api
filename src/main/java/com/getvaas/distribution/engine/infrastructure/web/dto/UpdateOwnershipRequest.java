package com.getvaas.distribution.engine.infrastructure.web.dto;

public record UpdateOwnershipRequest(
        UpdateOwnershipSourceRequest source,
        UpdateOwnershipCrossValidationRequest crossValidation
) {}
