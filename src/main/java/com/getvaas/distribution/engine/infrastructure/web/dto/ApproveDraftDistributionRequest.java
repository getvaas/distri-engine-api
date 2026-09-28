package com.getvaas.distribution.engine.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record ApproveDraftDistributionRequest(
        @NotNull Long companyId
) {}
