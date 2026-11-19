package com.yujing.content.dto;

import java.util.List;

public record UpdateInfoLinesRequest(List<LineInput> lines) {

    public record LineInput(String label, String value) {
    }
}
