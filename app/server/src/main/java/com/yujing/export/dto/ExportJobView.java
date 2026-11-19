package com.yujing.export.dto;

import java.time.OffsetDateTime;

public record ExportJobView(
        Long id,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        OffsetDateTime expiresAt,
        String downloadUrl
) {
}
