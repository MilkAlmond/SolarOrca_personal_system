package com.yujing.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Client metadata comes from transport headers, never from request bodies.
 * Unknown platforms are accepted for diagnostics but are not version-gated.
 */
public record ClientMeta(String platform, String appVersion, String deviceId, String ip) {

    public static final String HEADER_PLATFORM = "X-Client-Platform";
    public static final String HEADER_VERSION = "X-Client-Version";
    public static final String HEADER_DEVICE = "X-Device-Id";

    private static final Set<String> PLATFORMS = Set.of("web", "windows", "linux", "android", "unknown");
    private static final Pattern VERSION = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){0,3}$");

    public static ClientMeta from(HttpServletRequest request) {
        String platform = trimToNull(request.getHeader(HEADER_PLATFORM));
        platform = platform == null ? "unknown" : platform.toLowerCase(Locale.ROOT);
        if (!PLATFORMS.contains(platform)) {
            platform = "unknown";
        }

        String version = trimToNull(request.getHeader(HEADER_VERSION));
        if (version != null && !VERSION.matcher(version).matches()) {
            version = null;
        }

        String deviceId = sanitizeDeviceId(request.getHeader(HEADER_DEVICE));
        return new ClientMeta(platform, version, deviceId, clientIp(request));
    }

    public boolean web() {
        return "web".equals(platform);
    }

    private static String sanitizeDeviceId(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String cleaned = trimmed.replaceAll("[^A-Za-z0-9._:-]", "");
        return cleaned.isEmpty() ? null : cleaned.substring(0, Math.min(64, cleaned.length()));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String candidate = forwarded == null ? null : forwarded.split(",")[0].trim();
        if (candidate == null || candidate.isEmpty()) {
            candidate = request.getRemoteAddr();
        }
        if (candidate == null) {
            return null;
        }
        return candidate.substring(0, Math.min(45, candidate.length()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
