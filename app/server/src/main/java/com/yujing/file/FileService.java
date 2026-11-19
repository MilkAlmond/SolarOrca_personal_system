package com.yujing.file;

import com.yujing.audit.AuditService;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.file.dto.FileView;
import com.yujing.file.mapper.FileMapper;
import com.yujing.file.model.FileRow;
import com.yujing.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class FileService {

    private static final Set<String> PURPOSES = Set.of("avatar", "bio");

    private final FileStorage storage;
    private final FileMapper mapper;
    private final AuditService auditService;
    private final Clock clock;

    public FileService(FileStorage storage, FileMapper mapper, AuditService auditService, Clock clock) {
        this.storage = storage;
        this.mapper = mapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public FileView upload(MultipartFile file, String purpose, CurrentUser current, ClientMeta meta) {
        if (purpose == null || !PURPOSES.contains(purpose)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("purpose", "上传用途只能是 avatar 或 bio")));
        }
        FileStorage.StoredFile stored = storage.store(file);

        FileRow row = new FileRow();
        row.setOwnerId(current.id());
        row.setPurpose(purpose);
        row.setStoragePath(stored.relativePath());
        row.setContentType("image/png");
        row.setSizeBytes(stored.sizeBytes());
        row.setSha256(stored.sha256());
        row.setCreatedAt(LocalDateTime.now(clock));
        try {
            mapper.insert(row);
        } catch (RuntimeException ex) {
            storage.deleteQuietly(stored.relativePath());
            throw ex;
        }

        auditService.record(current.id(), "UPLOAD_FILE", "files/" + row.getId(), "success", meta.ip(), purpose);
        return new FileView(row.getId(), adminUrl(row.getId()), stored.sizeBytes());
    }

    public FileRow requireFile(Long id) {
        FileRow row = mapper.findActive(id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    public byte[] read(FileRow row) {
        return storage.read(row.getStoragePath());
    }

    public static String adminUrl(Long id) {
        return "/api/v1/admin/files/" + id;
    }
}
