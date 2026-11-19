package com.yujing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yujing")
public record AppProperties(
        String appVersion,
        Security security,
        Admin admin,
        Cors cors,
        Upload upload
) {

    public record Security(
            String cookieName,
            boolean cookieSecure,
            int cookieMaxAgeDays,
            int bcryptStrength,
            int lockThreshold,
            int lockMinutes
    ) {
    }

    public record Admin(String username, String password) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record Upload(String dir) {
    }
}
