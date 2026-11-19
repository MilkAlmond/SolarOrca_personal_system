package com.yujing.security;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The only source of the current owner id. Controllers and DTOs never accept
 * owner_id from clients (plan 5.3, 9.4).
 */
@Component
public class CurrentUserProvider {

    public CurrentUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
            return user;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    public Long requireId() {
        return require().id();
    }
}
