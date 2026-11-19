package com.yujing.auth.model;

import com.yujing.auth.dto.UserView;

public record AuthSession(String token, UserView user) {
}
