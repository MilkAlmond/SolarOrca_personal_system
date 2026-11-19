package com.yujing.content.dto;

import java.util.List;

public record PublicProfileView(String avatarUrl,
                                String title,
                                String subtitle,
                                String bio,
                                List<PublicImageRef> images) {
}
