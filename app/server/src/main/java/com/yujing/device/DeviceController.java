package com.yujing.device;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.device.dto.DeviceRequest;
import com.yujing.device.dto.DeviceView;
import com.yujing.device.dto.UpdateDeviceRequest;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final CurrentUserProvider currentUserProvider;

    public DeviceController(DeviceService deviceService, CurrentUserProvider currentUserProvider) {
        this.deviceService = deviceService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ApiResponse<DeviceView> register(@RequestBody(required = false) DeviceRequest body,
                                            HttpServletRequest request) {
        return ApiResponse.ok(deviceService.register(currentUserProvider.requireId(), body,
                ClientMeta.from(request).deviceId()));
    }

    @GetMapping
    public ApiResponse<List<DeviceView>> list() {
        return ApiResponse.ok(deviceService.list(currentUserProvider.requireId()));
    }

    @PatchMapping("/{deviceId}")
    public ApiResponse<DeviceView> updateNotifications(@PathVariable String deviceId,
                                                       @RequestBody UpdateDeviceRequest body) {
        return ApiResponse.ok(deviceService.updateNotifications(currentUserProvider.requireId(),
                deviceId, body));
    }
}
