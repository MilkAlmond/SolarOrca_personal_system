package com.yujing.admin;

import com.yujing.admin.dto.AuditLogView;
import com.yujing.audit.mapper.AuditLogMapper;
import com.yujing.audit.model.AuditLogRow;
import com.yujing.common.api.PageData;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AdminAuditService {

    private final AuditLogMapper mapper;

    public AdminAuditService(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    public PageData<AuditLogView> list(int page, int size, String action, String result, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeAction = trim(action);
        String safeResult = trim(result);
        String safeKeyword = trim(keyword);

        List<AuditLogView> items = mapper
                .findPage(safeAction, safeResult, safeKeyword, safeSize,
                        (long) (safePage - 1) * safeSize)
                .stream()
                .map(AdminAuditService::toView)
                .toList();
        return PageData.of(items, mapper.count(safeAction, safeResult, safeKeyword), safePage, safeSize);
    }

    private static AuditLogView toView(AuditLogRow row) {
        return new AuditLogView(row.getId(), row.getActorUserId(), row.getAction(), row.getTarget(),
                row.getResult(), row.getIp(), row.getDetail(),
                OffsetDateTime.of(row.getCreatedAt(), ZoneOffset.UTC));
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
