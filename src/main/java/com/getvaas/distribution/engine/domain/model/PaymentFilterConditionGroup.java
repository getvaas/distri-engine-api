package com.getvaas.distribution.engine.domain.model;

import java.util.List;

/**
 * Un grupo de condiciones unidas por AND. Los grupos entre sí se unen por OR — exactamente 2 niveles
 * de anidamiento, sin anidar más profundo (coincide 1:1 con el builder del mockup
 * {@code docs/screen-payments-filters.png}).
 */
public record PaymentFilterConditionGroup(
        List<PaymentFilterCondition> conditions
) {}
