package com.yujing.reminder.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateReminderRequest(
        @NotNull(message = "开关状态不能为空") Boolean enabled,
        @NotNull(message = "版本号不能为空") Long version) {
}
