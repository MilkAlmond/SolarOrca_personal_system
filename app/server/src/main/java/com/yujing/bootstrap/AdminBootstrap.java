package com.yujing.bootstrap;

import com.yujing.auth.PasswordPolicy;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.UserRow;
import com.yujing.common.error.BizException;
import com.yujing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * The admin account is pre-provisioned from environment variables and is never
 * created through registration. The password is only used when the account does
 * not exist yet, and is never written to logs or the repository.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties properties;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public AdminBootstrap(AppProperties properties,
                          UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          PasswordPolicy passwordPolicy,
                          Clock clock) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = trimToNull(properties.admin().username());
        String password = properties.admin().password();
        boolean hasPassword = password != null && !password.isBlank();

        if (username == null && !hasPassword) {
            log.warn("Admin bootstrap skipped: set YUJING_ADMIN_USERNAME and YUJING_ADMIN_PASSWORD to provision the owner account");
            return;
        }
        if (username == null || !hasPassword) {
            throw new IllegalStateException("YUJING_ADMIN_USERNAME and YUJING_ADMIN_PASSWORD must be set together");
        }
        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new IllegalStateException("Configured admin username must be 3-20 letters, digits or underscores");
        }
        try {
            passwordPolicy.validate("password", password);
        } catch (BizException ex) {
            throw new IllegalStateException("Configured admin password does not meet the password policy");
        }

        UserRow existing = userMapper.findByUsername(username);
        if (existing != null) {
            if (!"admin".equals(existing.getRole())) {
                throw new IllegalStateException("Configured admin username is already registered as a normal user");
            }
            log.info("Admin account already present: {}", username);
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        UserRow admin = new UserRow();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(username);
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        try {
            userMapper.insert(admin);
            log.info("Admin account created: {}", username);
        } catch (DuplicateKeyException ex) {
            log.warn("Admin account was created concurrently; leaving it unchanged");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
