package com.yujing.content;

import com.yujing.audit.AuditService;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.content.ContentValidator.NormalizedContact;
import com.yujing.content.ContentValidator.NormalizedHonor;
import com.yujing.content.ContentValidator.NormalizedLine;
import com.yujing.content.ContentValidator.NormalizedProfile;
import com.yujing.content.dto.AdminContact;
import com.yujing.content.dto.AdminHomeView;
import com.yujing.content.dto.AdminHonor;
import com.yujing.content.dto.AdminImageRef;
import com.yujing.content.dto.AdminInfoLine;
import com.yujing.content.dto.AdminProfileView;
import com.yujing.content.dto.UpdateContactsRequest;
import com.yujing.content.dto.UpdateHonorsRequest;
import com.yujing.content.dto.UpdateInfoLinesRequest;
import com.yujing.content.dto.UpdateProfileRequest;
import com.yujing.content.mapper.PublicContentMapper;
import com.yujing.content.model.ContactRow;
import com.yujing.content.model.PublicProfileRow;
import com.yujing.file.FileService;
import com.yujing.file.mapper.FileMapper;
import com.yujing.file.model.FileRow;
import com.yujing.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminContentService {

    private final PublicContentMapper mapper;
    private final FileMapper fileMapper;
    private final AuditService auditService;
    private final Clock clock;

    public AdminContentService(PublicContentMapper mapper, FileMapper fileMapper,
                               AuditService auditService, Clock clock) {
        this.mapper = mapper;
        this.fileMapper = fileMapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    public AdminHomeView home() {
        PublicProfileRow row = mapper.findProfile();
        List<AdminImageRef> images = mapper.listImageFileIds().stream()
                .map(id -> new AdminImageRef(id, FileService.adminUrl(id)))
                .toList();
        AdminProfileView profile = row == null ? null : new AdminProfileView(
                row.getAvatarFileId(),
                row.getAvatarFileId() == null ? null : FileService.adminUrl(row.getAvatarFileId()),
                row.getTitle(),
                row.getSubtitle(),
                row.getBio(),
                images);
        List<AdminInfoLine> lines = mapper.listRows().stream()
                .map(line -> new AdminInfoLine(line.getId(), line.getLabel(), line.getValue()))
                .toList();
        List<AdminHonor> honors = mapper.listHonors().stream()
                .map(honor -> new AdminHonor(honor.getId(), honor.getName(), honor.getAwardedOn()))
                .toList();
        List<AdminContact> contacts = mapper.listContacts().stream()
                .map(contact -> new AdminContact(contact.getId(), contact.getType(),
                        contact.getDisplayText(), contact.getTarget(), contact.isPublicVisible()))
                .toList();
        long version = row == null ? 0 : row.getVersion();
        return new AdminHomeView(mapper.countContent() > 0, version, profile, lines, honors, contacts);
    }

    @Transactional
    public AdminHomeView updateProfile(CurrentUser admin, UpdateProfileRequest request, ClientMeta meta) {
        NormalizedProfile profile = ContentValidator.validateProfile(request);

        Map<String, String> errors = new LinkedHashMap<>();
        if (profile.avatarFileId() != null) {
            requireFile(profile.avatarFileId(), "avatar", "avatarFileId", errors);
        }
        for (int i = 0; i < profile.imageFileIds().size(); i++) {
            requireFile(profile.imageFileIds().get(i), "bio", "imageFileIds[" + i + "]", errors);
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        PublicProfileRow existing = mapper.findProfile();
        if (existing == null) {
            if (request.version() != 0) {
                throw new BizException(ErrorCode.VERSION_CONFLICT);
            }
            try {
                mapper.insertProfile(profile.avatarFileId(), profile.title(), profile.subtitle(),
                        profile.bio(), admin.id(), now);
            } catch (DuplicateKeyException ex) {
                throw new BizException(ErrorCode.VERSION_CONFLICT);
            }
        } else if (mapper.updateProfile(profile.avatarFileId(), profile.title(), profile.subtitle(),
                profile.bio(), admin.id(), request.version(), now) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }

        mapper.deleteAllImages();
        for (int i = 0; i < profile.imageFileIds().size(); i++) {
            mapper.insertImage(profile.imageFileIds().get(i), i, now);
        }
        auditService.record(admin.id(), "PUBLISH_PROFILE", "public:home", "success", meta.ip(), null);
        return home();
    }

    @Transactional
    public AdminHomeView updateInfoLines(CurrentUser admin, UpdateInfoLinesRequest request, ClientMeta meta) {
        List<NormalizedLine> lines = ContentValidator.validateLines(request.lines());
        LocalDateTime now = LocalDateTime.now(clock);
        mapper.deleteAllRows();
        for (int i = 0; i < lines.size(); i++) {
            NormalizedLine line = lines.get(i);
            mapper.insertRow(line.label(), line.value(), i, now);
        }
        auditService.record(admin.id(), "PUBLISH_INFO", "public:home", "success", meta.ip(), null);
        return home();
    }

    @Transactional
    public AdminHomeView updateHonors(CurrentUser admin, UpdateHonorsRequest request, ClientMeta meta) {
        List<NormalizedHonor> honors = ContentValidator.validateHonors(request.items());
        LocalDateTime now = LocalDateTime.now(clock);
        mapper.deleteAllHonors();
        for (int i = 0; i < honors.size(); i++) {
            NormalizedHonor honor = honors.get(i);
            mapper.insertHonor(honor.name(), honor.awardedOn(), i, now);
        }
        auditService.record(admin.id(), "PUBLISH_HONORS", "public:home", "success", meta.ip(), null);
        return home();
    }

    @Transactional
    public AdminHomeView updateContacts(CurrentUser admin, UpdateContactsRequest request, ClientMeta meta) {
        List<NormalizedContact> contacts = ContentValidator.validateContacts(request.items());
        LocalDateTime now = LocalDateTime.now(clock);
        mapper.deleteAllContacts();
        for (int i = 0; i < contacts.size(); i++) {
            NormalizedContact contact = contacts.get(i);
            mapper.insertContact(contact.type(), contact.displayText(), contact.target(),
                    contact.publicVisible(), i, now);
        }
        auditService.record(admin.id(), "PUBLISH_CONTACTS", "public:home", "success", meta.ip(), null);
        return home();
    }

    private void requireFile(Long fileId, String purpose, String field, Map<String, String> errors) {
        FileRow file = fileMapper.findActive(fileId);
        if (file == null) {
            errors.put(field, "图片不存在");
        } else if (!purpose.equals(file.getPurpose())) {
            errors.put(field, "图片用途不正确");
        }
    }
}
