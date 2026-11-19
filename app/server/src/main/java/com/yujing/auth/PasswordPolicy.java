package com.yujing.auth;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PasswordPolicy {

    public void validate(String field, String rawPassword) {
        boolean valid = rawPassword != null
                && rawPassword.length() >= 8
                && rawPassword.length() <= 64
                && rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
        if (!valid) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of(field, "密码需为 8–64 个字符，且不超过 72 字节")));
        }
    }
}
