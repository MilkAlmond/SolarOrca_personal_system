package com.yujing.reminder.dto;

import java.time.OffsetDateTime;

public record ReminderView(
        Long id,
        String source,
        Long sourceId,
        String title,
        OffsetDateTime triggerAt,
        int advanceMinutes,
        boolean enabled,
        String deliveryStatus,
        OffsetDateTime sentAt,
        OffsetDateTime failedAt,
        long version,
        OffsetDateTime updatedAt) {
}
