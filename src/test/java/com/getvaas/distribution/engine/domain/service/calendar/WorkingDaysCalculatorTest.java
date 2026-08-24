package com.getvaas.distribution.engine.domain.service.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkingDaysCalculatorTest {

    private final WorkingDaysCalculator calculator = new WorkingDaysCalculator();

    @Test
    void subtractWorkingDays_zeroDays_returnsSameDate() {
        var date = LocalDate.of(2026, 8, 24); // lunes

        var result = calculator.subtractWorkingDays(date, 0, "Colombia (COL)");

        assertThat(result).isEqualTo(date);
    }

    @Test
    void subtractWorkingDays_withinSameWeek_skipsWeekendCorrectly() {
        // Lunes 24/8 - 1 día hábil = viernes 21/8 (no domingo 23)
        var date = LocalDate.of(2026, 8, 24);

        var result = calculator.subtractWorkingDays(date, 1, "Colombia (COL)");

        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 21));
    }

    @Test
    void subtractWorkingDays_crossingAWeekend_countsOnlyWorkingDays() {
        // Martes 25/8 - 3 días hábiles = jueves 20/8 (salta sáb 22 y dom 23)
        var date = LocalDate.of(2026, 8, 25);

        var result = calculator.subtractWorkingDays(date, 3, "Colombia (COL)");

        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void subtractWorkingDays_negativeDays_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> calculator.subtractWorkingDays(LocalDate.now(), -1, "Colombia (COL)"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
