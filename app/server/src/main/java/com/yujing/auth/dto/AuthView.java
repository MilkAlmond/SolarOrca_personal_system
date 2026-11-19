package com.yujing.auth.dto;

/**
 * token is null for web clients: those receive an HttpOnly cookie instead of a
 * script-readable value (plan 5.2). Native clients receive the token here.
 */
public record AuthView(String token, UserView user) {
}
