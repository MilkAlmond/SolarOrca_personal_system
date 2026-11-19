package com.yujing.birthday.dto;

import java.time.LocalDate;

public record BirthdayRequest(
        String name,
        String category,
        String calendarType,
        LocalDate solarDate,
        Integer lunarMonth,
        Integer lunarDay,
        Boolean lunarLeap,
        String relationTag,
        Integer advanceDays,
        String note,
        Long version) {
}
