package com.yujing.content.dto;

public record AdminContact(Long id, String type, String displayText, String target, boolean publicVisible) {
}
