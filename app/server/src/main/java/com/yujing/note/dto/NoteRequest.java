package com.yujing.note.dto;

public record NoteRequest(
        String title,
        String body,
        String category,
        Long version) {
}
