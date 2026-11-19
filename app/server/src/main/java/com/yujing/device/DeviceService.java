package com.yujing.device;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.device.dto.DeviceRequest;
import com.yujing.device.dto.DeviceView;
import com.yujing.device.dto.UpdateDeviceRequest;
import com.yujing.device.mapper.DeviceMapper;
import com.yujing.device.model.DeviceRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DeviceService {

    private static final Set<String> PLATFORMS = Set.of("web", "windows", "linux", "android", "unknown");
    private static final Pattern DEVICE_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    private final DeviceMapper mapper;
    private final Clock clock;

    public DeviceService(DeviceMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DeviceView register(Long ownerId, DeviceRequest request, String headerDeviceId) {
        String deviceId = firstNonBlank(request == null ? null : request.deviceId(), headerDeviceId);
        if (deviceId == null || !DEVICE_ID.matcher(deviceId).matches()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("deviceId", "设备编号需为 1–64 位字母、数字或 . _ : -")));
        }
        String platform = normalizePlatform(request == null ? null : request.platform());
        String appVersion = trim(request == null ? null : request.appVersion());
        if (appVersion != null && appVersion.length() > 20) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("appVersion", "版本号不能超过 20 位")));
        }
        boolean notificationsEnabled = request == null || request.notificationsEnabled() == null
                || request.notificationsEnabled();

        mapper.upsert(ownerId, deviceId, platform, appVersion, notificationsEnabled,
                LocalDateTime.now(clock));
        DeviceRow row = mapper.findByDeviceId(ownerId, deviceId);
        if (row == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        return toView(row);
    }

    public List<DeviceView> list(Long ownerId) {
        return mapper.findAll(ownerId).stream().map(DeviceService::toView).toList();
    }

    @Transactional
    public DeviceView updateNotifications(Long ownerId, String deviceId, UpdateDeviceRequest request) {
        if (request == null || request.notificationsEnabled() == null || request.version() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("notificationsEnabled", "请提供开关状态和版本号")));
        }
        if (mapper.updateNotifications(ownerId, deviceId, request.notificationsEnabled(),
                request.version(), LocalDateTime.now(clock)) == 0) {
            DeviceRow existing = mapper.findByDeviceId(ownerId, deviceId);
            if (existing == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        return toView(mapper.findByDeviceId(ownerId, deviceId));
    }

    private static String normalizePlatform(String platform) {
        String value = platform == null ? null : platform.strip().toLowerCase();
        return value != null && PLATFORMS.contains(value) ? value : "unknown";
    }

    private static String firstNonBlank(String first, String second) {
        String value = trim(first);
        return value != null ? value : trim(second);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static DeviceView toView(DeviceRow row) {
        return new DeviceView(row.getDeviceId(), row.getPlatform(), row.getAppVersion(),
                row.isNotificationsEnabled(),
                OffsetDateTime.of(row.getLastSeenAt(), ZoneOffset.UTC), row.getVersion());
    }
}
