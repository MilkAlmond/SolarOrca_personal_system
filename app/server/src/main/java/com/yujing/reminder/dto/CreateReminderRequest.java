package com.yujing.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateReminderRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 40, message = "标题不能超过 40 字")
        String title,
        @NotNull(message = "触发时间不能为空")
        OffsetDateTime triggerAt) {
}
