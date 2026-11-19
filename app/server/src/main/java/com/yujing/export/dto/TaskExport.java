package com.yujing.export.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Full task record for the export bundle, including its check-ins and milestones. */
public record TaskExport(
        Long id,
        Long ownerId,
        String name,
        String period,
        Integer weeklyTarget,
        LocalDate startDate,
        LocalTime remindTime,
        boolean enabled,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<LocalDate> checkinDates,
        List<MilestoneExport> milestones
) {

    public record MilestoneExport(Integer days, LocalDate reachedOn) {
    }
}
