package com.yujing.security;

import com.yujing.auth.mapper.SessionMapper;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.SessionRow;
import com.yujing.auth.model.UserRow;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.web.ClientMeta;
import com.yujing.common.web.JsonErrorWriter;
import com.yujing.version.AppVersionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final SessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final AuthCookieService cookieService;
    private final JsonErrorWriter errorWriter;
    private final AppVersionService appVersionService;
    private final Clock clock;

    public TokenAuthFilter(SessionMapper sessionMapper,
                           UserMapper userMapper,
                           TokenService tokenService,
                           AuthCookieService cookieService,
                           JsonErrorWriter errorWriter,
                           AppVersionService appVersionService,
                           Clock clock) {
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.cookieService = cookieService;
        this.errorWriter = errorWriter;
        this.appVersionService = appVersionService;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        boolean fromCookie = false;

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            token = cookieService.read(request);
            fromCookie = token != null;
        }
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        SessionRow session = sessionMapper.findActiveByTokenHash(tokenService.hash(token));
        if (session == null) {
            if (fromCookie) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
            }
            filterChain.doFilter(request, response);
            return;
        }

        UserRow user = userMapper.findById(session.getUserId());
        if (user == null) {
            if (fromCookie) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!"active".equals(user.getStatus())) {
            sessionMapper.revokeById(session.getId(), LocalDateTime.now(clock), "account_disabled");
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
            errorWriter.write(response, ErrorCode.ACCOUNT_DISABLED);
            return;
        }

        CurrentUser principal = new CurrentUser(user.getId(), user.getUsername(),
                user.getDisplayName(), user.getRole(), session.getId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(Locale.ROOT))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (!isReadMethod(request.getMethod()) && !isAuthEndpoint(request.getRequestURI())) {
            ClientMeta meta = ClientMeta.from(request);
            if (appVersionService.isReadOnly(meta.platform(), meta.appVersion())) {
                errorWriter.write(response, ErrorCode.READ_ONLY_MODE);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isReadMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private boolean isAuthEndpoint(String uri) {
        return uri != null && uri.startsWith("/api/v1/auth/");
    }
}
