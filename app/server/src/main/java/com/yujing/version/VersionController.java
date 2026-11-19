package com.yujing.version;

import com.yujing.common.api.ApiResponse;
import com.yujing.version.dto.VersionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
public class VersionController {

    private final AppVersionService appVersionService;

    public VersionController(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
    }

    @GetMapping("/version")
    public ApiResponse<VersionView> version(@RequestParam String platform,
                                            @RequestParam("current") String current) {
        return ApiResponse.ok(appVersionService.check(platform, current));
    }
}
