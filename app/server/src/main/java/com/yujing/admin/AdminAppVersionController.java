package com.yujing.admin;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.CurrentUserProvider;
import com.yujing.version.AppVersionService;
import com.yujing.version.dto.UpdateVersionRequest;
import com.yujing.version.dto.VersionView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/app-versions")
public class AdminAppVersionController {

    private final AppVersionService appVersionService;
    private final CurrentUserProvider currentUserProvider;

    public AdminAppVersionController(AppVersionService appVersionService,
                                     CurrentUserProvider currentUserProvider) {
        this.appVersionService = appVersionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<List<VersionView>> list() {
        return ApiResponse.ok(appVersionService.list());
    }

    @PutMapping("/{platform}")
    public ApiResponse<VersionView> update(@PathVariable String platform,
                                           @RequestBody UpdateVersionRequest body,
                                           HttpServletRequest request) {
        return ApiResponse.ok(appVersionService.update(platform, body,
                currentUserProvider.require(), ClientMeta.from(request)));
    }
}
