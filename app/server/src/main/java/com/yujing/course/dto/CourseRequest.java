package com.yujing.course.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CourseRequest(
        String name,
        String teacher,
        String location,
        Integer weekday,
        LocalTime startTime,
        LocalTime endTime,
        String repeatType,
        LocalDate courseDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Integer remindMinutes,
        Long version) {
}
