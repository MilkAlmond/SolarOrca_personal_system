package com.yujing.birthday.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BirthdayView(
        Long id,
        String name,
        String category,
        String calendarType,
        LocalDate solarDate,
        Integer lunarMonth,
        Integer lunarDay,
        boolean lunarLeap,
        String relationTag,
        int advanceDays,
        String note,
        LocalDate nextDate,
        long daysUntil,
        boolean leapAdjusted,
        long version,
        OffsetDateTime updatedAt) {
}
