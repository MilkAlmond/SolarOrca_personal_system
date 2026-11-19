package com.yujing.course.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record CourseView(
        Long id,
        String name,
        String teacher,
        String location,
        int weekday,
        LocalTime startTime,
        LocalTime endTime,
        String repeatType,
        LocalDate courseDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        int remindMinutes,
        long version,
        OffsetDateTime updatedAt) {
}
