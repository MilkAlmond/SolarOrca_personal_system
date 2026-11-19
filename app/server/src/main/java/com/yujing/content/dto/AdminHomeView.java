package com.yujing.content.dto;

import java.util.List;

public record AdminHomeView(boolean published,
                            long version,
                            AdminProfileView profile,
                            List<AdminInfoLine> infoLines,
                            List<AdminHonor> honors,
                            List<AdminContact> contacts) {
}
