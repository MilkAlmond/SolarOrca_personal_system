package com.yujing.common.api;

import java.util.List;

public record PageData<T>(List<T> items, long total, int page, int size) {

    public static <T> PageData<T> of(List<T> items, long total, int page, int size) {
        return new PageData<>(items, total, page, size);
    }
}
