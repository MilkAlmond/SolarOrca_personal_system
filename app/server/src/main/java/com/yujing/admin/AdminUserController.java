package com.yujing.admin;

import com.yujing.admin.dto.AdminUserView;
import com.yujing.admin.dto.UpdateUserStatusRequest;
import com.yujing.common.api.ApiResponse;
import com.yujing.common.api.PageData;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserProvider currentUserProvider;

    public AdminUserController(AdminUserService adminUserService,
                               CurrentUserProvider currentUserProvider) {
        this.adminUserService = adminUserService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageData<AdminUserView>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminUserService.list(page, size, keyword));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminUserView> updateStatus(@PathVariable Long id,
                                                   @RequestBody UpdateUserStatusRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.updateStatus(currentUserProvider.require(), id,
                request, ClientMeta.from(httpRequest)));
    }
}
