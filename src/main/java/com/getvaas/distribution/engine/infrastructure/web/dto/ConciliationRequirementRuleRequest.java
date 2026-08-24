package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.enums.ConciliationTable;

public record ConciliationRequirementRuleRequest(
        ConciliationTable tableA,
        ConciliationTable tableB,
        String gateway
) {}
