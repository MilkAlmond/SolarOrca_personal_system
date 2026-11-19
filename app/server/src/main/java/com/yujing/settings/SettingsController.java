package com.yujing.settings;

import com.yujing.common.api.ApiResponse;
import com.yujing.settings.dto.SettingsView;
import com.yujing.settings.dto.UpdateSettingsRequest;
import com.yujing.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUserProvider currentUserProvider;

    public SettingsController(SettingsService settingsService, CurrentUserProvider currentUserProvider) {
        this.settingsService = settingsService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<SettingsView> get() {
        return ApiResponse.ok(settingsService.get(currentUserProvider.requireId()));
    }

    @PatchMapping
    public ApiResponse<SettingsView> update(@Valid @RequestBody UpdateSettingsRequest body) {
        return ApiResponse.ok(settingsService.update(currentUserProvider.requireId(), body.remindersEnabled()));
    }
}
