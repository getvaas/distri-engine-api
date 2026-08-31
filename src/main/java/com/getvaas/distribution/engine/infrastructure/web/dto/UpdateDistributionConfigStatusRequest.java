package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;

public record UpdateDistributionConfigStatusRequest(
        DistributionConfigStatus status
) {}
