package com.yujing.auth;

import com.yujing.auth.dto.AuthView;
import com.yujing.auth.dto.ChangePasswordRequest;
import com.yujing.auth.dto.LoginRequest;
import com.yujing.auth.dto.RegisterRequest;
import com.yujing.auth.dto.UpdateProfileRequest;
import com.yujing.auth.dto.UserView;
import com.yujing.auth.model.AuthSession;
import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.security.AuthCookieService;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService,
                          AuthCookieService cookieService,
                          CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthView>> register(@Valid @RequestBody RegisterRequest body,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        ClientMeta meta = ClientMeta.from(request);
        AuthSession session = authService.register(body, meta);
        return authResponse(session, meta, response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthView>> login(@Valid @RequestBody LoginRequest body,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        ClientMeta meta = ClientMeta.from(request);
        AuthSession session = authService.login(body, meta);
        return authResponse(session, meta, response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(currentUserProvider.require(), ClientMeta.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
        return ApiResponse.ok();
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body,
                                            HttpServletRequest request) {
        authService.changePassword(currentUserProvider.require(), body, ClientMeta.from(request));
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.ok(authService.me(currentUserProvider.require()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserView> updateProfile(@Valid @RequestBody UpdateProfileRequest body,
                                               HttpServletRequest request) {
        return ApiResponse.ok(authService.updateProfile(currentUserProvider.require(), body,
                ClientMeta.from(request)));
    }

    private ResponseEntity<ApiResponse<AuthView>> authResponse(AuthSession session,
                                                               ClientMeta meta,
                                                               HttpServletResponse response) {
        if (meta.web()) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(session.token()).toString());
            return ResponseEntity.ok(ApiResponse.ok(new AuthView(null, session.user())));
        }
        return ResponseEntity.ok(ApiResponse.ok(new AuthView(session.token(), session.user())));
    }
}
