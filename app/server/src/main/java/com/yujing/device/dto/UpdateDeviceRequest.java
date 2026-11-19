package com.yujing.device.dto;

public record UpdateDeviceRequest(Boolean notificationsEnabled, Long version) {
}
