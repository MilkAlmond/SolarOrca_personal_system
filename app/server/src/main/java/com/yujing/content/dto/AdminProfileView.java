package com.yujing.content.dto;

import java.util.List;

public record AdminProfileView(Long avatarFileId,
                               String avatarUrl,
                               String title,
                               String subtitle,
                               String bio,
                               List<AdminImageRef> images) {
}
