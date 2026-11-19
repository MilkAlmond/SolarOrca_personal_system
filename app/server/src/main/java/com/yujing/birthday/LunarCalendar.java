package com.yujing.birthday;

import java.time.LocalDate;

/**
 * Minimal Chinese lunar to solar conversion (1900–2100). The table encodes the
 * leap month (lowest 4 bits) and the 12 month lengths (bits 15..4) of each lunar
 * year; bit 16 is the length of the leap month.
 *
 * Only conversion is needed for B1; lunar dates are stored as month/day plus a
 * leap flag and converted every year to place them on the solar calendar.
 */
public final class LunarCalendar {

    private static final LocalDate BASE = LocalDate.of(1900, 1, 31);

    private static final int[] INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
            0x0d520
    };

    private static final int FIRST_YEAR = 1900;
    private static final int LAST_YEAR = FIRST_YEAR + INFO.length - 1;

    private LunarCalendar() {
    }

    public static boolean supports(int lunarYear) {
        return lunarYear >= FIRST_YEAR && lunarYear <= LAST_YEAR;
    }

    public static int leapMonth(int lunarYear) {
        requireYear(lunarYear);
        return INFO[lunarYear - FIRST_YEAR] & 0xF;
    }

    public static int leapMonthDays(int lunarYear) {
        int leap = leapMonth(lunarYear);
        if (leap == 0) {
            return 0;
        }
        return (INFO[lunarYear - FIRST_YEAR] & 0x10000) != 0 ? 30 : 29;
    }

    public static int monthDays(int lunarYear, int month) {
        requireYear(lunarYear);
        if (month < 1 || month > 12) {
            return 0;
        }
        return (INFO[lunarYear - FIRST_YEAR] & (0x10000 >> month)) != 0 ? 30 : 29;
    }

    public static int yearDays(int lunarYear) {
        int days = 0;
        for (int month = 1; month <= 12; month++) {
            days += monthDays(lunarYear, month);
        }
        return days + leapMonthDays(lunarYear);
    }

    /**
     * Converts a lunar date to its solar equivalent. A leap request for a month
     * that is not leap in that year falls back to the regular month, and a day
     * beyond the month length is clamped to the last day (common convention).
     */
    public static LocalDate toSolar(int lunarYear, int lunarMonth, int lunarDay, boolean leap) {
        requireYear(lunarYear);
        int month = Math.max(1, Math.min(12, lunarMonth));
        int leapMonth = leapMonth(lunarYear);
        boolean useLeap = leap && leapMonth == month;
        int monthLength = useLeap ? leapMonthDays(lunarYear) : monthDays(lunarYear, month);
        int day = Math.max(1, Math.min(Math.max(monthLength, 1), lunarDay));

        int offset = 0;
        for (int year = FIRST_YEAR; year < lunarYear; year++) {
            offset += yearDays(year);
        }
        for (int m = 1; m < month; m++) {
            offset += monthDays(lunarYear, m);
            if (leapMonth > 0 && m == leapMonth) {
                offset += leapMonthDays(lunarYear);
            }
        }
        if (useLeap) {
            offset += monthDays(lunarYear, month);
        }
        offset += day - 1;
        return BASE.plusDays(offset);
    }

    private static void requireYear(int lunarYear) {
        if (!supports(lunarYear)) {
            throw new IllegalArgumentException("Lunar year out of supported range: " + lunarYear);
        }
    }
}
