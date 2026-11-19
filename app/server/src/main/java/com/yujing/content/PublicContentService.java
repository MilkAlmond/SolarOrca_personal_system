package com.yujing.content;

import com.yujing.content.dto.PublicContact;
import com.yujing.content.dto.PublicHomeView;
import com.yujing.content.dto.PublicHonor;
import com.yujing.content.dto.PublicImageRef;
import com.yujing.content.dto.PublicInfoLine;
import com.yujing.content.dto.PublicProfileView;
import com.yujing.content.mapper.PublicContentMapper;
import com.yujing.content.model.ContactRow;
import com.yujing.content.model.PublicProfileRow;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicContentService {

    private final PublicContentMapper mapper;

    public PublicContentService(PublicContentMapper mapper) {
        this.mapper = mapper;
    }

    public PublicHomeView home() {
        PublicProfileRow row = mapper.findProfile();
        List<PublicImageRef> images = mapper.listImageFileIds().stream()
                .map(id -> new PublicImageRef(id, publicUrl(id)))
                .toList();
        PublicProfileView profile = row == null ? null : new PublicProfileView(
                row.getAvatarFileId() == null ? null : publicUrl(row.getAvatarFileId()),
                row.getTitle(),
                row.getSubtitle(),
                row.getBio(),
                images);
        List<PublicInfoLine> lines = mapper.listRows().stream()
                .map(line -> new PublicInfoLine(line.getLabel(), line.getValue()))
                .toList();
        List<PublicHonor> honors = mapper.listHonors().stream()
                .map(honor -> new PublicHonor(honor.getName(), honor.getAwardedOn()))
                .toList();
        List<PublicContact> contacts = mapper.listContacts().stream()
                .filter(ContactRow::isPublicVisible)
                .map(contact -> new PublicContact(contact.getType(), contact.getDisplayText(), contact.getTarget()))
                .toList();
        return new PublicHomeView(mapper.countContent() > 0, profile, lines, honors, contacts);
    }

    /**
     * Draft uploads never become public; only files referenced by the published
     * singleton content are served under /api/v1/public/files.
     */
    public boolean isPublishedFile(Long fileId) {
        return mapper.countAvatarRef(fileId) + mapper.countImageRef(fileId) > 0;
    }

    public static String publicUrl(Long fileId) {
        return "/api/v1/public/files/" + fileId;
    }
}
