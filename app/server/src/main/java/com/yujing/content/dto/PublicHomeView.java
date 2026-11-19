package com.yujing.content.dto;

import java.util.List;

public record PublicHomeView(boolean published,
                             PublicProfileView profile,
                             List<PublicInfoLine> infoLines,
                             List<PublicHonor> honors,
                             List<PublicContact> contacts) {
}
