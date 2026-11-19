package com.yujing.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "账号名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
