package com.yujing.birthday;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LunarCalendarTest {

    @Test
    void convertsKnownSpringFestivals() {
        assertEquals(LocalDate.of(2023, 1, 22), LunarCalendar.toSolar(2023, 1, 1, false));
        assertEquals(LocalDate.of(2024, 2, 10), LunarCalendar.toSolar(2024, 1, 1, false));
        assertEquals(LocalDate.of(2025, 1, 29), LunarCalendar.toSolar(2025, 1, 1, false));
        assertEquals(LocalDate.of(2026, 2, 17), LunarCalendar.toSolar(2026, 1, 1, false));
    }

    @Test
    void convertsKnownMidAutumn() {
        assertEquals(LocalDate.of(2024, 9, 17), LunarCalendar.toSolar(2024, 8, 15, false));
        assertEquals(LocalDate.of(2025, 10, 6), LunarCalendar.toSolar(2025, 8, 15, false));
    }

    @Test
    void detectsLeapMonths() {
        assertEquals(2, LunarCalendar.leapMonth(2023));
        assertEquals(0, LunarCalendar.leapMonth(2024));
        assertEquals(6, LunarCalendar.leapMonth(2025));
        assertEquals(0, LunarCalendar.leapMonth(2026));
    }

    @Test
    void convertsLeapMonth() {
        assertEquals(LocalDate.of(2023, 3, 22), LunarCalendar.toSolar(2023, 2, 1, true));
    }

    @Test
    void clampsDayThirtyToShortMonth() {
        int monthDays = LunarCalendar.monthDays(2026, 1);
        LocalDate lastDay = LunarCalendar.toSolar(2026, 1, 30, false);
        LocalDate expected = LunarCalendar.toSolar(2026, 1, monthDays, false);
        assertEquals(expected, lastDay);
    }
}
