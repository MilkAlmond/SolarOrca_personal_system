package com.yujing.account;

import com.yujing.account.dto.CancelDeletionRequest;
import com.yujing.account.dto.DeletionRequest;
import com.yujing.account.dto.DeletionStatusView;
import com.yujing.audit.AuditService;
import com.yujing.auth.mapper.SessionMapper;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.UserRow;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Plan 9.6: deletion freezes the account for 30 days and revokes every session.
 * A frozen account cannot log in; cancellation is a dedicated public endpoint
 * that verifies the username and password without creating a session.
 */
@Service
public class AccountService {

    private static final int FREEZE_DAYS = 30;

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public AccountService(UserMapper userMapper,
                          SessionMapper sessionMapper,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService,
                          Clock clock) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = BizException.class)
    public DeletionStatusView requestDeletion(CurrentUser current, DeletionRequest request, ClientMeta meta) {
        UserRow user = userMapper.findById(current.id());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.password() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.record(user.getId(), "ACCOUNT_DELETE_REQUEST", "user:" + user.getId(),
                    "failed", meta.ip(), "password");
            throw new BizException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime scheduledAt = now.plusDays(FREEZE_DAYS);
        userMapper.updateDeletion(user.getId(), now, scheduledAt, now);
        sessionMapper.revokeAll(user.getId(), now, "account_deletion");
        auditService.record(user.getId(), "ACCOUNT_DELETE_REQUEST", "user:" + user.getId(),
                "success", meta.ip(), null);
        return new DeletionStatusView(true, utc(now), utc(scheduledAt));
    }

    public DeletionStatusView status(Long ownerId) {
        UserRow user = userMapper.findById(ownerId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return new DeletionStatusView(user.getDeletionScheduledAt() != null,
                utc(user.getDeletionRequestedAt()), utc(user.getDeletionScheduledAt()));
    }

    @Transactional(noRollbackFor = BizException.class)
    public void cancel(CancelDeletionRequest request, ClientMeta meta) {
        String username = request == null || request.username() == null ? null : request.username().strip();
        String password = request == null ? null : request.password();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("username", "请输入账号名和密码")));
        }

        UserRow user = userMapper.findByUsernameForUpdate(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            auditService.record(user == null ? null : user.getId(), "ACCOUNT_DELETE_CANCEL", username,
                    "failed", meta.ip(), "credentials");
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getDeletionScheduledAt() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("username", "账号未申请注销")));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        userMapper.clearDeletion(user.getId(), now);
        auditService.record(user.getId(), "ACCOUNT_DELETE_CANCEL", username, "success", meta.ip(), null);
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : OffsetDateTime.of(value, ZoneOffset.UTC);
    }
}
