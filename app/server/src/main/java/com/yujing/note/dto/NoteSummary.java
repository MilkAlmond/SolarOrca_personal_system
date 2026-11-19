package com.yujing.note.dto;

import java.time.OffsetDateTime;

public record NoteSummary(
        Long id,
        String title,
        String category,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
