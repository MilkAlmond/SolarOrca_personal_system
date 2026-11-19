package com.yujing.device.dto;

import java.time.OffsetDateTime;

public record DeviceView(
        String deviceId,
        String platform,
        String appVersion,
        boolean notificationsEnabled,
        OffsetDateTime lastSeenAt,
        long version
) {
}
