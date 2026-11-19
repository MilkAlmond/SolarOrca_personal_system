package com.yujing.device.dto;

public record DeviceRequest(
        String deviceId,
        String platform,
        String appVersion,
        Boolean notificationsEnabled
) {
}
