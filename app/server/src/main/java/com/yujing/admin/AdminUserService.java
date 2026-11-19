package com.yujing.admin;

import com.yujing.admin.dto.AdminUserView;
import com.yujing.admin.dto.UpdateUserStatusRequest;
import com.yujing.admin.mapper.AdminUserMapper;
import com.yujing.audit.AuditService;
import com.yujing.auth.mapper.SessionMapper;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.UserRow;
import com.yujing.common.api.PageData;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> STATUSES = Set.of("active", "disabled");

    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final AuditService auditService;
    private final Clock clock;

    public AdminUserService(AdminUserMapper adminUserMapper, UserMapper userMapper,
                            SessionMapper sessionMapper, AuditService auditService, Clock clock) {
        this.adminUserMapper = adminUserMapper;
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    public PageData<AdminUserView> list(int page, int size, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String pattern = keyword == null || keyword.isBlank() ? null : "%" + keyword.strip() + "%";
        List<AdminUserView> items = adminUserMapper
                .findPage(pattern, safeSize, (long) (safePage - 1) * safeSize)
                .stream()
                .map(AdminUserService::toView)
                .toList();
        return PageData.of(items, adminUserMapper.count(pattern), safePage, safeSize);
    }

    @Transactional
    public AdminUserView updateStatus(CurrentUser admin, Long userId, UpdateUserStatusRequest request,
                                      ClientMeta meta) {
        String status = request.status() == null ? null : request.status().strip();
        if (status == null || !STATUSES.contains(status)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("status", "状态只能是 active 或 disabled")));
        }
        if (userId.equals(admin.id()) && "disabled".equals(status)) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    Map.of("fields", Map.of("status", "不能停用当前登录的管理员账号")));
        }
        UserRow user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        adminUserMapper.updateStatus(userId, status, now);
        if ("disabled".equals(status)) {
            sessionMapper.revokeAll(userId, now, "disabled");
        }
        user.setStatus(status);
        auditService.record(admin.id(), "disabled".equals(status) ? "DISABLE_USER" : "ENABLE_USER",
                "user:" + userId, "success", meta.ip(), null);
        return toView(user);
    }

    private static AdminUserView toView(UserRow user) {
        return new AdminUserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.getStatus(), user.getLastLoginAt(), user.getCreatedAt());
    }
}
