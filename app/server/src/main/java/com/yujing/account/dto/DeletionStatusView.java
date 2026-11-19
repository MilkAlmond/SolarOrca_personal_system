package com.yujing.account.dto;

import java.time.OffsetDateTime;

public record DeletionStatusView(
        boolean frozen,
        OffsetDateTime requestedAt,
        OffsetDateTime scheduledAt
) {
}
