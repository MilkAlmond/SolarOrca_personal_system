package com.yujing.admin.dto;

import java.time.OffsetDateTime;

public record AuditLogView(
        Long id,
        Long actorUserId,
        String action,
        String target,
        String result,
        String ip,
        String detail,
        OffsetDateTime createdAt
) {
}
