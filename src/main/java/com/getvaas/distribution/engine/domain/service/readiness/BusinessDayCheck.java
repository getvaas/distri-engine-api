package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

/**
 * Hoy solo valida fin de semana. Falta el calendario de feriados por país (ver
 * {@code DateHelper.getWorkingDaysBack} en {@code master-trust-servicer-api} para la referencia real) —
 * se agrega cuando un deal lo necesite, no antes.
 */
@Component
public class BusinessDayCheck implements ReadinessCheck {

    @Override
    public ReadinessCheckType type() {
        return ReadinessCheckType.BUSINESS_DAY;
    }

    @Override
    public ReadinessCheckResult evaluate(ReadinessCheckContext context) {
        DayOfWeek dayOfWeek = context.date().getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.FAILED,
                    context.date() + " es " + dayOfWeek + ", no es día hábil");
        }
        return new ReadinessCheckResult(type(), ReadinessCheckStatus.PASSED, null);
    }
}
