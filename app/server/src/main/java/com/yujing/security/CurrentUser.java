package com.yujing.security;

public record CurrentUser(Long id, String username, String displayName, String role, Long sessionId) {

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
