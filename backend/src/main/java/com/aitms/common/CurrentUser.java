package com.aitms.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aitms.security.AuthUser;

/**
 * 현재 사용자 — Spring Security 세션 로그인의 principal({@link AuthUser}).
 * 인증 컨텍스트가 없는 곳(단위/서비스 테스트, 백그라운드 스레드)은 기본 사용자(id 1, qa.kim)로 대체.
 * 웹 요청(/api/**)은 모두 로그인이 필요하므로 이 대체 경로는 사실상 타지 않음.
 */
public final class CurrentUser {

    private static final Long DEFAULT_USER_ID = 1L; // data.sql 의 qa.kim

    private CurrentUser() {
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthUser user && "ADMIN".equals(user.role());
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user.id();
        }
        return DEFAULT_USER_ID;
    }
}
