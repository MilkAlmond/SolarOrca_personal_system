package com.yujing.birthday;

import com.yujing.birthday.BirthdaySchedule.Occurrence;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirthdayScheduleTest {

    @Test
    void februaryTwentyNineFallsBackToTwentyEight() {
        Occurrence occurrence = BirthdaySchedule.next(LocalDate.of(2000, 2, 29), null, null, false,
                LocalDate.of(2026, 2, 27));
        assertEquals(LocalDate.of(2026, 2, 28), occurrence.date());
        assertTrue(occurrence.leapAdjusted());
    }

    @Test
    void februaryTwentyNineUsesRealDayInLeapYear() {
        Occurrence occurrence = BirthdaySchedule.next(LocalDate.of(2000, 2, 29), null, null, false,
                LocalDate.of(2024, 2, 1));
        assertEquals(LocalDate.of(2024, 2, 29), occurrence.date());
        assertFalse(occurrence.leapAdjusted());
    }

    @Test
    void todayCountsAsNextOccurrence() {
        Occurrence occurrence = BirthdaySchedule.next(LocalDate.of(1990, 3, 15), null, null, false,
                LocalDate.of(2026, 3, 15));
        assertEquals(LocalDate.of(2026, 3, 15), occurrence.date());
        assertEquals(0, BirthdaySchedule.daysUntil(occurrence.date(), LocalDate.of(2026, 3, 15)));
    }

    @Test
    void pastOccurrenceRollsToNextYear() {
        Occurrence occurrence = BirthdaySchedule.next(LocalDate.of(1990, 3, 15), null, null, false,
                LocalDate.of(2026, 3, 16));
        assertEquals(LocalDate.of(2027, 3, 15), occurrence.date());
        assertEquals(364, BirthdaySchedule.daysUntil(occurrence.date(), LocalDate.of(2026, 3, 16)));
    }

    @Test
    void lunarOccurrenceUsesNextSolarYear() {
        Occurrence occurrence = BirthdaySchedule.next(null, 8, 15, false, LocalDate.of(2024, 9, 1));
        assertEquals(LocalDate.of(2024, 9, 17), occurrence.date());
        assertFalse(occurrence.leapAdjusted());
    }

    @Test
    void lunarLeapMonthFallsBackWhenYearHasNoLeapMonth() {
        // 2023 has leap month 2; 2024 does not, so the regular 2nd month is used.
        Occurrence occurrence = BirthdaySchedule.next(null, 2, 1, true, LocalDate.of(2024, 1, 10));
        assertEquals(LocalDate.of(2024, 3, 10), occurrence.date());
        assertTrue(occurrence.leapAdjusted());
    }
}
