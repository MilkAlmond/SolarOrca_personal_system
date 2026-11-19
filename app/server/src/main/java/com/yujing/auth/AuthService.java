package com.yujing.auth;

import com.yujing.audit.AuditService;
import com.yujing.auth.dto.ChangePasswordRequest;
import com.yujing.auth.dto.LoginRequest;
import com.yujing.auth.dto.RegisterRequest;
import com.yujing.auth.dto.UserView;
import com.yujing.auth.mapper.SessionMapper;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.AuthSession;
import com.yujing.auth.model.SessionRow;
import com.yujing.auth.model.UserRow;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.config.AppProperties;
import com.yujing.security.CurrentUser;
import com.yujing.security.TokenService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final AppProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserMapper userMapper,
                       SessionMapper sessionMapper,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy,
                       TokenService tokenService,
                       AuditService auditService,
                       AppProperties properties,
                       Clock clock) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("unused-" + UUID.randomUUID());
    }

    @Transactional
    public AuthSession register(RegisterRequest request, ClientMeta meta) {
        passwordPolicy.validate("password", request.password());
        String username = request.username();
        if (userMapper.findByUsername(username) != null) {
            throw new BizException(ErrorCode.USERNAME_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        UserRow user = new UserRow();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(username);
        user.setRole("user");
        user.setStatus("active");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.USERNAME_TAKEN);
        }

        auditService.record(user.getId(), "REGISTER", username, "success", meta.ip(), null);
        return createSession(user, meta, now);
    }

    @Transactional(noRollbackFor = BizException.class)
    public AuthSession login(LoginRequest request, ClientMeta meta) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserRow user = userMapper.findByUsernameForUpdate(request.username());

        if (user == null) {
            // Constant-ish work for unknown accounts to reduce username probing.
            passwordEncoder.matches(request.password(), dummyHash);
            auditService.record(null, "LOGIN", request.username(), "failed", meta.ip(), "unknown_account");
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!"active".equals(user.getStatus())) {
            auditService.record(user.getId(), "LOGIN", user.getUsername(), "failed", meta.ip(), "disabled");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            // Do not reveal whether the password was correct while locked.
            auditService.record(user.getId(), "LOGIN", user.getUsername(), "failed", meta.ip(), "locked");
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getLockedUntil() != null) {
            userMapper.resetLock(user.getId(), now);
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int failedCount = user.getFailedLoginCount() + 1;
            LocalDateTime lockedUntil = failedCount >= properties.security().lockThreshold()
                    ? now.plusMinutes(properties.security().lockMinutes())
                    : null;
            userMapper.markLoginFailure(user.getId(), failedCount, lockedUntil, now);
            auditService.record(user.getId(), "LOGIN", user.getUsername(), "failed", meta.ip(), "bad_password");
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }

        userMapper.markLoginSuccess(user.getId(), now);
        auditService.record(user.getId(), "LOGIN", user.getUsername(), "success", meta.ip(), null);
        return createSession(user, meta, now);
    }

    @Transactional
    public void logout(CurrentUser current, ClientMeta meta) {
        sessionMapper.revokeById(current.sessionId(), LocalDateTime.now(clock), "logout");
        auditService.record(current.id(), "LOGOUT", current.username(), "success", meta.ip(), null);
    }

    @Transactional(noRollbackFor = BizException.class)
    public void changePassword(CurrentUser current, ChangePasswordRequest request, ClientMeta meta) {
        UserRow user = userMapper.findById(current.id());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.record(current.id(), "PASSWORD_CHANGE", current.username(), "failed", meta.ip(),
                    "current_password");
            throw new BizException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        passwordPolicy.validate("newPassword", request.newPassword());

        LocalDateTime now = LocalDateTime.now(clock);
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()), now);
        sessionMapper.revokeOthers(user.getId(), current.sessionId(), now, "password_change");
        auditService.record(current.id(), "PASSWORD_CHANGE", current.username(), "success", meta.ip(), null);
    }

    public UserView me(CurrentUser current) {
        UserRow user = userMapper.findById(current.id());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return toView(user);
    }

    private AuthSession createSession(UserRow user, ClientMeta meta, LocalDateTime now) {
        String rawToken = tokenService.newToken();
        SessionRow session = new SessionRow();
        session.setUserId(user.getId());
        session.setTokenHash(tokenService.hash(rawToken));
        session.setDeviceId(meta.deviceId() == null ? UUID.randomUUID().toString() : meta.deviceId());
        session.setPlatform(meta.platform());
        session.setAppVersion(meta.appVersion());
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        sessionMapper.insert(session);

        UserView view = toView(user);
        return new AuthSession(rawToken, view);
    }

    private UserView toView(UserRow user) {
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
