package com.yujing.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 32, message = "显示名不能超过 32 字")
        String displayName
) {
}
