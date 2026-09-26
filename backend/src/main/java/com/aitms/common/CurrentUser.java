package com.aitms.common;

/** 로그인 사용자. TODO: Spring Security 도입 시 SecurityContext에서 조회하도록 교체. */
public final class CurrentUser {

    private static final Long DEFAULT_USER_ID = 1L; // data.sql 의 qa01

    private CurrentUser() {
    }

    public static Long id() {
        return DEFAULT_USER_ID;
    }
}
