package com.yujing.content.dto;

import java.util.List;

public record UpdateProfileRequest(Long avatarFileId,
                                   String title,
                                   String subtitle,
                                   String bio,
                                   List<Long> imageFileIds,
                                   long version) {
}
