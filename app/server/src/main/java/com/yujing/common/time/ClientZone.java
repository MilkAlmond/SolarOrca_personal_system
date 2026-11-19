package com.yujing.common.time;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * The client reports its UTC offset on every request (plan 6.3 "接口传偏移量").
 * The offset is only used to interpret wall-clock dates and times; storage stays
 * in UTC. Out-of-range or missing values fall back to UTC.
 */
public record ClientZone(ZoneOffset offset) {

    public static final String HEADER_OFFSET = "X-Client-Offset-Minutes";

    private static final int MIN_MINUTES = -12 * 60;
    private static final int MAX_MINUTES = 14 * 60;

    public static ClientZone from(HttpServletRequest request) {
        String raw = request == null ? null : request.getHeader(HEADER_OFFSET);
        return ofMinutes(parseMinutes(raw));
    }

    public static ClientZone ofMinutes(Integer minutes) {
        int safe = minutes == null ? 0 : Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes));
        return new ClientZone(ZoneOffset.ofTotalSeconds(safe * 60));
    }

    public int offsetMinutes() {
        return offset.getTotalSeconds() / 60;
    }

    public LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(offset));
    }

    public LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock.withZone(offset));
    }

    public ZoneOffset zone() {
        return offset;
    }

    private static Integer parseMinutes(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
