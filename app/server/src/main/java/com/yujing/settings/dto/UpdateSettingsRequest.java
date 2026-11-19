package com.yujing.settings.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequest(
        @NotNull(message = "提醒总开关不能为空")
        Boolean remindersEnabled
) {
}
