package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record UpdateDistributionRulesRequest(
        Boolean hasComponentOwners,
        List<ComponentOwnerRuleRequest> componentOwners
) {}
