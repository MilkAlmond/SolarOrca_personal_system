package com.yujing.version.dto;

public record UpdateVersionRequest(
        String latestVersion,
        String minSupportedVersion,
        String downloadUrl,
        String notes
) {
}
