package com.yujing.birthday;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure date rules for B1 (plan 3.2): yearly repetition, lunar conversion and the
 * 02-29 fallback in non-leap years.
 */
public final class BirthdaySchedule {

    private BirthdaySchedule() {
    }

    public record Occurrence(LocalDate date, boolean leapAdjusted) {
    }

    public static Occurrence next(LocalDate solarDate, Integer lunarMonth, Integer lunarDay,
                                  boolean lunarLeap, LocalDate today) {
        if (solarDate != null) {
            return nextSolar(solarDate, today);
        }
        return nextLunar(lunarMonth, lunarDay, lunarLeap, today);
    }

    private static Occurrence nextSolar(LocalDate solarDate, LocalDate today) {
        int month = solarDate.getMonthValue();
        int day = solarDate.getDayOfMonth();
        int year = today.getYear();
        for (int i = 0; i < 3; i++, year++) {
            LocalDate candidate;
            boolean adjusted = false;
            if (month == 2 && day == 29 && !java.time.Year.isLeap(year)) {
                candidate = LocalDate.of(year, 2, 28);
                adjusted = true;
            } else {
                candidate = LocalDate.of(year, month, day);
            }
            if (!candidate.isBefore(today)) {
                return new Occurrence(candidate, adjusted);
            }
        }
        return new Occurrence(LocalDate.of(year, month, Math.min(day, 28)), false);
    }

    private static Occurrence nextLunar(Integer lunarMonth, Integer lunarDay, boolean lunarLeap,
                                        LocalDate today) {
        if (lunarMonth == null || lunarDay == null) {
            return null;
        }
        BirthdaySchedule.Occurrence best = null;
        int firstYear = Math.max(1900, today.getYear() - 1);
        int lastYear = Math.min(2100, today.getYear() + 2);
        for (int lunarYear = firstYear; lunarYear <= lastYear; lunarYear++) {
            boolean leapAvailable = LunarCalendar.leapMonth(lunarYear) == lunarMonth;
            LocalDate candidate = LunarCalendar.toSolar(lunarYear, lunarMonth, lunarDay,
                    lunarLeap && leapAvailable);
            if (candidate.isBefore(today)) {
                continue;
            }
            boolean adjusted = lunarLeap && !leapAvailable;
            if (best == null || candidate.isBefore(best.date())) {
                best = new BirthdaySchedule.Occurrence(candidate, adjusted);
            }
        }
        return best;
    }

    public static long daysUntil(LocalDate occurrence, LocalDate today) {
        return ChronoUnit.DAYS.between(today, occurrence);
    }
}
