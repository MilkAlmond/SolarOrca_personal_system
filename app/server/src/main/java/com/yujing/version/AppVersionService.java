package com.yujing.version;

import com.yujing.audit.AuditService;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.CurrentUser;
import com.yujing.version.dto.UpdateVersionRequest;
import com.yujing.version.dto.VersionView;
import com.yujing.version.mapper.AppVersionMapper;
import com.yujing.version.model.AppVersionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AppVersionService {

    private static final Set<String> PLATFORMS = Set.of("web", "windows", "linux", "android");

    private final AppVersionMapper mapper;
    private final AuditService auditService;
    private final Clock clock;

    public AppVersionService(AppVersionMapper mapper, AuditService auditService, Clock clock) {
        this.mapper = mapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    public VersionView check(String platform, String currentVersion) {
        if (!PLATFORMS.contains(platform)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("platform", "平台必须是 web、windows、linux 或 android")));
        }
        if (!Semver.isValid(currentVersion)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("current", "版本号格式不正确")));
        }
        AppVersionRow row = mapper.findByPlatform(platform);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        boolean readOnly = Semver.compare(currentVersion, row.getMinSupportedVersion()) < 0;
        return new VersionView(row.getPlatform(), row.getLatestVersion(), row.getMinSupportedVersion(),
                row.getDownloadUrl(), row.getNotes(), readOnly);
    }

    public boolean isReadOnly(String platform, String currentVersion) {
        if (platform == null || currentVersion == null || !PLATFORMS.contains(platform)
                || !Semver.isValid(currentVersion)) {
            return false;
        }
        AppVersionRow row = mapper.findByPlatform(platform);
        return row != null && Semver.compare(currentVersion, row.getMinSupportedVersion()) < 0;
    }

    public List<VersionView> list() {
        return mapper.findAll().stream()
                .map(row -> new VersionView(row.getPlatform(), row.getLatestVersion(),
                        row.getMinSupportedVersion(), row.getDownloadUrl(), row.getNotes(), false))
                .toList();
    }

    @Transactional
    public VersionView update(String platform, UpdateVersionRequest request, CurrentUser admin,
                              ClientMeta meta) {
        if (!PLATFORMS.contains(platform)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("platform", "平台必须是 web、windows、linux 或 android")));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String latest = trim(request == null ? null : request.latestVersion());
        String minSupported = trim(request == null ? null : request.minSupportedVersion());
        if (!Semver.isValid(latest)) {
            errors.put("latestVersion", "版本号格式不正确");
        }
        if (!Semver.isValid(minSupported)) {
            errors.put("minSupportedVersion", "版本号格式不正确");
        }
        String downloadUrl = trim(request == null ? null : request.downloadUrl());
        if (downloadUrl != null && downloadUrl.length() > 500) {
            errors.put("downloadUrl", "下载地址不能超过 500 字");
        }
        String notes = trim(request == null ? null : request.notes());
        if (notes != null && notes.codePointCount(0, notes.length()) > 500) {
            errors.put("notes", "更新说明不能超过 500 字");
        }
        if (errors.isEmpty() && Semver.compare(latest, minSupported) < 0) {
            errors.put("minSupportedVersion", "最低支持版本不能高于最新版本");
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.update(platform, latest, minSupported, downloadUrl, notes, now) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        auditService.record(admin.id(), "VERSION_UPDATE", "app_version:" + platform, "success",
                meta.ip(), latest);
        return new VersionView(platform, latest, minSupported, downloadUrl, notes, false);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
