package com.yujing.account;

import com.yujing.account.dto.CancelDeletionRequest;
import com.yujing.account.dto.DeletionRequest;
import com.yujing.account.dto.DeletionStatusView;
import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.AuthCookieService;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthCookieService cookieService;

    public AccountController(AccountService accountService,
                             CurrentUserProvider currentUserProvider,
                             AuthCookieService cookieService) {
        this.accountService = accountService;
        this.currentUserProvider = currentUserProvider;
        this.cookieService = cookieService;
    }

    @PostMapping("/deletion")
    public ApiResponse<DeletionStatusView> requestDeletion(@RequestBody DeletionRequest body,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        DeletionStatusView view = accountService.requestDeletion(currentUserProvider.require(), body,
                ClientMeta.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
        return ApiResponse.ok(view);
    }

    @GetMapping("/deletion")
    public ApiResponse<DeletionStatusView> status() {
        return ApiResponse.ok(accountService.status(currentUserProvider.requireId()));
    }

    @PostMapping("/deletion/cancel")
    public ApiResponse<Void> cancel(@RequestBody CancelDeletionRequest body,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        accountService.cancel(body, ClientMeta.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
        return ApiResponse.ok();
    }
}
