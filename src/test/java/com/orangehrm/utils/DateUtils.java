package com.orangehrm.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DateUtils {
    private DateUtils() {
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate addWorkingDays(LocalDate date, int workingDays) {
        if (workingDays < 0) {
            throw new IllegalArgumentException("Working days must not be negative");
        }

        LocalDate result = date;
        int added = 0;
        while (added < workingDays) {
            result = result.plusDays(1);
            DayOfWeek day = result.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
