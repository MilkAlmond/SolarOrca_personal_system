package com.yujing.admin.dto;

import java.time.LocalDateTime;

public record AdminUserView(Long id,
                            String username,
                            String displayName,
                            String role,
                            String status,
                            LocalDateTime lastLoginAt,
                            LocalDateTime createdAt) {
}
