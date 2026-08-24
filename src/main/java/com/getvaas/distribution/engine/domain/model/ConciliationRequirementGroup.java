package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Un grupo de reglas de conciliación unidas por AND. Los grupos entre sí se unen por OR — mismo
 * patrón de 2 niveles que {@link PaymentFilterConditionGroup}.
 */
public record ConciliationRequirementGroup(
        List<ConciliationRequirementRule> rules
) {}
