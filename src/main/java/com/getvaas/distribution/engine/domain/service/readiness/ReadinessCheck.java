package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

/**
 * Una precondición evaluable antes de distribuir. Cada implementación es un {@code @Component} —
 * {@link ReadinessCheckRunner} las descubre todas y las indexa por {@link #type()}.
 */
public interface ReadinessCheck {

    ReadinessCheckType type();

    ReadinessCheckResult evaluate(ReadinessCheckContext context);
}
