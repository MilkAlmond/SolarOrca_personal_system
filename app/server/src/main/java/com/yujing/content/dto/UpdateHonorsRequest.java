package com.yujing.content.dto;

import java.util.List;

public record UpdateHonorsRequest(List<HonorInput> items) {

    public record HonorInput(String name, String awardedOn) {
    }
}
