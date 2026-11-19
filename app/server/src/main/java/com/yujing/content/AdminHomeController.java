package com.yujing.content;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.content.dto.AdminHomeView;
import com.yujing.content.dto.UpdateContactsRequest;
import com.yujing.content.dto.UpdateHonorsRequest;
import com.yujing.content.dto.UpdateInfoLinesRequest;
import com.yujing.content.dto.UpdateProfileRequest;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/home")
public class AdminHomeController {

    private final AdminContentService adminContentService;
    private final CurrentUserProvider currentUserProvider;

    public AdminHomeController(AdminContentService adminContentService,
                               CurrentUserProvider currentUserProvider) {
        this.adminContentService = adminContentService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<AdminHomeView> home() {
        return ApiResponse.ok(adminContentService.home());
    }

    @PutMapping("/profile")
    public ApiResponse<AdminHomeView> updateProfile(@RequestBody UpdateProfileRequest request,
                                                    HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminContentService.updateProfile(currentUserProvider.require(),
                request, ClientMeta.from(httpRequest)));
    }

    @PutMapping("/info-lines")
    public ApiResponse<AdminHomeView> updateInfoLines(@RequestBody UpdateInfoLinesRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminContentService.updateInfoLines(currentUserProvider.require(),
                request, ClientMeta.from(httpRequest)));
    }

    @PutMapping("/honors")
    public ApiResponse<AdminHomeView> updateHonors(@RequestBody UpdateHonorsRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminContentService.updateHonors(currentUserProvider.require(),
                request, ClientMeta.from(httpRequest)));
    }

    @PutMapping("/contacts")
    public ApiResponse<AdminHomeView> updateContacts(@RequestBody UpdateContactsRequest request,
                                                     HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminContentService.updateContacts(currentUserProvider.require(),
                request, ClientMeta.from(httpRequest)));
    }
}
