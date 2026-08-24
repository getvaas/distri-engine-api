package com.getvaas.distribution.engine.domain.service.calendar;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Retrocede N días hábiles desde una fecha. Hoy solo excluye fines de semana — falta el calendario de
 * feriados por país (mismo TODO que {@code BusinessDayCheck}, VPR-9661). El parámetro {@code country}
 * ya se recibe para no tener que cambiar la firma cuando se agregue el calendario real.
 */
@Component
public class WorkingDaysCalculator {

    public LocalDate subtractWorkingDays(LocalDate date, int days, String country) {
        if (days < 0) {
            throw new IllegalArgumentException("days no puede ser negativo: " + days);
        }
        LocalDate result = date;
        int remaining = days;
        while (remaining > 0) {
            result = result.minusDays(1);
            if (isWorkingDay(result)) {
                remaining--;
            }
        }
        return result;
    }

    private boolean isWorkingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
