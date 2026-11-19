package com.yujing.admin;

import com.yujing.admin.dto.AuditLogView;
import com.yujing.common.api.ApiResponse;
import com.yujing.common.api.PageData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public ApiResponse<PageData<AuditLogView>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) String action,
                                                    @RequestParam(required = false) String result,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminAuditService.list(page, size, action, result, keyword));
    }
}
