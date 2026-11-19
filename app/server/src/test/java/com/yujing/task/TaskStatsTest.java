package com.yujing.task;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskStatsTest {

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 18),
                LocalDate.of(2026, 11, 19));
        assertEquals(3, TaskStats.streak(dates, LocalDate.of(2026, 11, 19)));
    }

    @Test
    void streakStillCountsYesterdayWhenTodayMissing() {
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 18));
        assertEquals(2, TaskStats.streak(dates, LocalDate.of(2026, 11, 19)));
    }

    @Test
    void missedDayBreaksStreak() {
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 16));
        assertEquals(0, TaskStats.streak(dates, LocalDate.of(2026, 11, 19)));
    }

    @Test
    void weekProgressUsesMondayToSunday() {
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 19),
                LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 11, 23));
        assertEquals(4, TaskStats.weekProgress(dates, LocalDate.of(2026, 11, 19)));
    }
}
