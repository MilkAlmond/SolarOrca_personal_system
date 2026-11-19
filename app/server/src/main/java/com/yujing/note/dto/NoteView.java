package com.yujing.note.dto;

import java.time.OffsetDateTime;

public record NoteView(
        Long id,
        String title,
        String body,
        String category,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
