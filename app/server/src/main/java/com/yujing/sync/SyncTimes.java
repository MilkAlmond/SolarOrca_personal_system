package com.yujing.sync;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public final class SyncTimes {

    private SyncTimes() {
    }

    /**
     * Accepts ISO-8601 with an offset (2026-11-19T10:00:00Z or +08:00) and
     * converts it to the UTC local timestamp used by the database.
     */
    public static LocalDateTime parseSince(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw.strip())
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (RuntimeException ex) {
            try {
                return LocalDateTime.parse(raw.strip());
            } catch (RuntimeException ignored) {
                throw new BizException(ErrorCode.VALIDATION_FAILED,
                        Map.of("fields", Map.of("since", "时间格式需为 ISO-8601")));
            }
        }
    }

    public static String serverTime(Clock clock) {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).toString();
    }

    public static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : OffsetDateTime.of(value, ZoneOffset.UTC);
    }
}
