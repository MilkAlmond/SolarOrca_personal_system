package com.yujing.version;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.version.dto.VersionView;
import com.yujing.version.mapper.AppVersionMapper;
import com.yujing.version.model.AppVersionRow;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class AppVersionService {

    private static final Set<String> PLATFORMS = Set.of("web", "windows", "linux", "android");

    private final AppVersionMapper mapper;

    public AppVersionService(AppVersionMapper mapper) {
        this.mapper = mapper;
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
}
