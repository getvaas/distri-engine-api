package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDayCheckTest {

    private final BusinessDayCheck check = new BusinessDayCheck();

    @Test
    void evaluate_weekday_passes() {
        var monday = LocalDate.of(2026, 8, 24); // lunes
        var context = new ReadinessCheckContext(3L, monday, "Colombia (COL)");

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        assertThat(result.reason()).isNull();
    }

    @Test
    void evaluate_saturday_fails() {
        var saturday = LocalDate.of(2026, 8, 22);
        var context = new ReadinessCheckContext(3L, saturday, "Colombia (COL)");

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
        assertThat(result.reason()).contains("SATURDAY");
    }

    @Test
    void evaluate_sunday_fails() {
        var sunday = LocalDate.of(2026, 8, 23);
        var context = new ReadinessCheckContext(3L, sunday, "Colombia (COL)");

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
    }
}
