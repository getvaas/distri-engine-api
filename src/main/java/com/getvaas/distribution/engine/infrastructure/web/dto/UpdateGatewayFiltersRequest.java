package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;

import java.util.List;

public record UpdateGatewayFiltersRequest(
        GatewayFilterMode mode,
        List<String> gateways
) {}
