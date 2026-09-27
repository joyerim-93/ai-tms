package com.aitms.common;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aitms.domain.user.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * '누가 입력했는지 표시용' — 화면이 보내는 X-User-Name(URL 인코딩) 헤더의 이름을 users 행으로 찾거나 만들어
 * 이 요청의 CurrentUser 로 지정. 인증이 아니므로 검증하지 않음. Spring Security 도입 시 제거.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-User-Name";

    private final UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String raw = request.getHeader(HEADER);
            if (raw != null && !raw.isBlank()) {
                String name = decode(raw).strip();
                if (!name.isEmpty()) {
                    CurrentUser.set(userService.resolveByName(name));
                }
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }

    private static String decode(String raw) {
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return raw;
        }
    }
}
