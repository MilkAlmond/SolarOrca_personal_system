package com.yujing.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "账号名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "账号名需为 3–20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为 8–64 位")
        String password
) {
}
