package com.yujing.task.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskRequest(
        String name,
        String period,
        Integer weeklyTarget,
        LocalDate startDate,
        LocalTime remindTime,
        Boolean enabled,
        Long version) {
}
