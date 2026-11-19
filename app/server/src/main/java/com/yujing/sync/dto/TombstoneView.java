package com.yujing.sync.dto;

import java.time.OffsetDateTime;

public record TombstoneView(
        String entityType,
        Long entityId,
        long entityVersion,
        OffsetDateTime deletedAt) {
}
