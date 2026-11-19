package com.yujing.content.dto;

import java.time.LocalDate;

public record AdminHonor(Long id, String name, LocalDate awardedOn) {
}
