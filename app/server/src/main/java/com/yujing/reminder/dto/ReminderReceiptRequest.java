package com.yujing.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReminderReceiptRequest(
        @Size(max = 64, message = "设备编号过长") String deviceId,
        @NotBlank(message = "结果不能为空") String result) {
}
