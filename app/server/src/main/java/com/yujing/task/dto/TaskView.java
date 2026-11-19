package com.yujing.task.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record TaskView(
        Long id,
        String name,
        String period,
        Integer weeklyTarget,
        LocalDate startDate,
        LocalTime remindTime,
        boolean enabled,
        int streakDays,
        boolean checkedToday,
        int weekProgress,
        boolean weekCompleted,
        List<Integer> milestones,
        long version,
        OffsetDateTime updatedAt) {
}
