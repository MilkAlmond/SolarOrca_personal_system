package com.yujing.security;

import com.yujing.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    private final AppProperties properties;

    public AuthCookieService(AppProperties properties) {
        this.properties = properties;
    }

    public String cookieName() {
        return properties.security().cookieName();
    }

    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from(cookieName(), rawToken)
                .httpOnly(true)
                .secure(properties.security().cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(properties.security().cookieMaxAgeDays()))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(cookieName(), "")
                .httpOnly(true)
                .secure(properties.security().cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName().equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
