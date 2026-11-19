package com.yujing.content.dto;

import java.util.List;

public record UpdateContactsRequest(List<ContactInput> items) {

    public record ContactInput(String type, String displayText, String target, Boolean publicVisible) {
    }
}
