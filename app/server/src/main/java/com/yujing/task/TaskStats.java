package com.yujing.task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

/** Pure check-in statistics so the streak and weekly-progress rules stay testable. */
public final class TaskStats {

    private TaskStats() {
    }

    /**
     * Consecutive days ending at {@code date} when checked in that day, otherwise
     * ending the day before. Missing a single day breaks the run.
     */
    public static int streak(Set<LocalDate> dates, LocalDate date) {
        LocalDate cursor = dates.contains(date) ? date : date.minusDays(1);
        int count = 0;
        while (dates.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    /** Check-ins inside the Monday–Sunday week that contains {@code date}. */
    public static int weekProgress(Set<LocalDate> dates, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (dates.contains(monday.plusDays(i))) {
                count++;
            }
        }
        return count;
    }
}
