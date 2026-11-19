package com.yujing.version.dto;

public record VersionView(
        String platform,
        String latestVersion,
        String minSupportedVersion,
        String downloadUrl,
        String notes,
        boolean readOnly
) {
}
